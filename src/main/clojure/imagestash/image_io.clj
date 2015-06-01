(ns imagestash.image-io
  (:import [java.util Arrays]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel]
           [imagestash.j FileChannels])
  (:require [imagestash.image-core :refer :all]
            [imagestash.flags :as flags]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(defn- write-padding-buffer [^ByteBuffer buffer]
  (let [pos (.position buffer)
        len (padding-amount pos)]
    (dotimes [_ len]
      (.put buffer (byte 0)))))

(defn- write-header-to-buffer [^ByteBuffer buffer {:keys [flags key-length size format data-length]
                                                   :or   {flags #{}}} checksum]
  {:pre [(pos? key-length)
         (pos? data-length)
         (pos? size)
         (io/byte-array? checksum)
         (= d/digest-length (alength checksum))]}
  (let [format-code (format-to-code (format/parse-format format))
        encoded-flags (flags/encode-flags flags)]
    (doto buffer
      (.put preamble-bytes)
      (.put encoded-flags)
      (.putShort (short size))
      (.put (byte format-code))
      (.putShort (short key-length))
      (.putInt data-length)
      (.put checksum))))

(defn- update-digest-with-header [digest {:keys [flags key-length size format data-length]
                                          :or   {flags #{}}}]
  {:pre [(pos? key-length)
         (pos? data-length)
         (pos? size)]}
  (let [format-code (format-to-code (format/parse-format format))
        encoded-flags (flags/encode-flags flags)]
    (d/update-digest digest
                     preamble-bytes
                     encoded-flags
                     size
                     format-code
                     key-length
                     data-length)))

(defn- write-image-to-buffer [^ByteBuffer buffer image]
  (let [digest (d/new-digest)
        original-position (.position buffer)
        image-key (:key image)
        key-bytes (.getBytes image-key charset)
        image-data (:data image)
        header (assoc image
                 :key-length (alength key-bytes)
                 :data-length (alength image-data))]
    (update-digest-with-header digest header)
    (d/update-digest digest key-bytes image-data)
    (doto buffer
      (write-header-to-buffer header (d/get-digest digest))
      (.put key-bytes)
      (.put image-data)
      (write-padding-buffer))
    (let [storage-size (- (.position buffer) original-position)]
      {:storage-size storage-size
       :checksum     (d/get-digest digest)})))

(defn write-image-to-file [target {:keys [flags key size format data]
                                   :or   {flags #{}} :as image}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [channel (FileChannels/append target)]
    (assert (= (.size channel) (.position channel))
            "Channel is not opened at file end")
    (let [position (.position channel)
          size (stored-image-size image)
          buffer (ByteBuffer/allocate size)
          written-image (write-image-to-buffer buffer image)]
      (assert (= size (:storage-size written-image))
              "Calculated image record size differs from actual record size")
      (assert (= 0 (.remaining buffer))
              "Less data was written to buffer as expected")
      (.rewind buffer)
      (.write channel buffer position)
      (assoc written-image :offset position))))

(defn- read-header-from-buffer [^ByteBuffer buffer]
  (let [stored-preamble (io/read-bytes-from-buffer buffer preamble-size)]
    (when-not (Arrays/equals preamble-bytes stored-preamble)
      (throw (ex-info "Invalid image preamble" {:preamble stored-preamble})))
    (let [encoded-flags (.get buffer)
          image-size (.getShort buffer)
          format-code (.get buffer)
          key-len (.getShort buffer)
          data-len (.getInt buffer)
          checksum (io/read-bytes-from-buffer buffer d/digest-length)
          image-format (code-to-format format-code)]
      {:flags       (flags/decode-flags encoded-flags)
       :key-length  key-len
       :size        image-size
       :format      image-format
       :data-length data-len
       :checksum    checksum})))

(defn read-image-from-buffer [^ByteBuffer buffer]
  (let [digest (d/new-digest)
        original-pos (.position buffer)
        header (read-header-from-buffer buffer)
        key-bytes (io/read-bytes-from-buffer buffer (:key-length header))
        image-key (String. key-bytes charset)
        image-data (io/read-bytes-from-buffer buffer (:data-length header))
        padding-len (padding-amount (.position buffer))
        _ (io/skip-buffer buffer padding-len)
        storage-size (- (.position buffer) original-pos)
        checksum (:checksum header)]
    (update-digest-with-header digest header)
    (d/update-digest digest key-bytes image-data)
    (d/verify-checksum (d/get-digest digest) checksum)
    (assoc header
           :key image-key
           :data image-data
           :storage-size storage-size)))

(defn read-image-from-channel [^FileChannel channel position size]
  {:pre [(pos? size)
         (<= (+ position size) (.size channel))]}
  (let [buffer (io/read-from-channel channel position size)]
    (read-image-from-buffer buffer)))

(defn read-image-from-channel-without-size [^FileChannel channel position]
  (let [digest (d/new-digest)
        header-buffer (io/read-from-channel channel position header-size)
        header (read-header-from-buffer header-buffer)
        pad-amount (padding-amount (+ header-size (:key-length header) (:data-length header)))
        payload-size (+ (:key-length header) (:data-length header) pad-amount)
        payload-buffer (io/read-from-channel channel (+ position header-size) payload-size)
        key-bytes (io/read-bytes-from-buffer payload-buffer (:key-length header))
        image-key (String. key-bytes charset)
        image-data (io/read-bytes-from-buffer payload-buffer (:data-length header))
        checksum (:checksum header)
        storage-size (+ header-size payload-size)]
    (update-digest-with-header digest header)
    (d/update-digest digest key-bytes image-data)
    (d/verify-checksum (d/get-digest digest) checksum)
    (assoc header
      :key image-key
      :data image-data
      :storage-size storage-size)))

(defn read-image-from-file [source position size]
  {:pre [(pos? size)]}
  (with-open [channel (FileChannels/read source)]
    (read-image-from-channel channel position size)))