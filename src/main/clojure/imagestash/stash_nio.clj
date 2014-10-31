(ns imagestash.stash-nio
  (:import [java.io RandomAccessFile ByteArrayInputStream]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel])
  (:require [imagestash.stash-core :refer :all]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(defn- write-padding-buffer [^ByteBuffer buffer]
  (let [pos (.position buffer)
        len (padding-length pos)]
    (dotimes [i len]
      (.put buffer (byte 0)))))

(defn- write-header-to-buffer [^ByteBuffer buffer digest {:keys [flags key size format data]
                                                          :or   {flags 0}}]
  (let [key-bytes (.getBytes key charset)
        format-code (format-to-code (format/parse-format format))]
    (doto buffer
      (.put header-bytes)
      (.put (unchecked-byte flags))
      (.putShort (short (alength key-bytes)))
      (.put key-bytes)
      (.putShort (short size))
      (.put (byte format-code))
      (.putInt (alength data)))
    (d/update-digest digest
                     header-bytes
                     flags
                     (alength key-bytes)
                     key-bytes
                     size
                     format-code
                     (alength data))))

(defn- write-data-to-buffer [^ByteBuffer buffer digest data]
  (.put buffer data)
  (d/update-digest digest data))

(defn- write-image-to-buffer [^ByteBuffer buffer {:keys [flags key size format data]
                                                  :or   {flags 0} :as header}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (let [digest (d/new-digest)
        original-length (.position buffer)]
    (doto buffer
      (write-header-to-buffer digest header)
      (write-data-to-buffer digest data)
      (.put (d/get-digest digest))
      (write-padding-buffer))
    (let [storage-size (- (.position buffer) original-length)]
      {:size     storage-size
       :checksum (d/get-digest digest)})))

(defn write-image-to-file [target {:keys [flags key size format data]
                                   :or   {flags 0} :as image}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [ra-file (RandomAccessFile. target "rw")]
    (let [original-length (.length ra-file)]
      (let [size (size-on-disk image)
            buffer (ByteBuffer/allocate size)
            channel (.getChannel ra-file)
            written-image (write-image-to-buffer buffer image)]
        (assert (= size (:size written-image)))
        (assert (= 0 (.remaining buffer)))
        (.rewind buffer)
        (.write channel buffer original-length)
        (assoc written-image :offset original-length)))))

(defn- read-header-from-buffer [^ByteBuffer buffer digest]
  (let [header-on-disk (io/read-bytes-from-buffer buffer (alength header-bytes))]
    (when-not (Arrays/equals header-bytes header-on-disk)
      (throw (ex-info "Invalid header" {:header header-on-disk})))
    (let [flags (.get buffer)
          key-len (.getShort buffer)
          key-buf (io/read-bytes-from-buffer buffer key-len)
          image-key (String. key-buf charset)
          image-size (.getShort buffer)
          format-code (.get buffer)
          image-format (code-to-format format-code)
          data-len (.getInt buffer)]
      (d/update-digest digest header-bytes flags key-len key-buf image-size format-code data-len)
      {:flags       flags
       :key         image-key
       :size        image-size
       :format      image-format
       :data-length data-len})))

(defn read-image-from-buffer [^ByteBuffer buffer]
  (let [digest (d/new-digest)
        original-pos (.position buffer)
        header (read-header-from-buffer buffer digest)
        image-data (io/read-bytes-from-buffer buffer (:data-length header))
        checksum (io/read-bytes-from-buffer buffer d/digest-length)
        padding-len (padding-length (.position buffer))
        _ (io/skip-buffer buffer padding-len)
        stored-len (- (.position buffer) original-pos)
        _ (d/update-digest digest image-data)
        expected-checksum (d/get-digest digest)]
    (when (not (Arrays/equals expected-checksum checksum))
      (throw (ex-info "Image checksum does not match" {:key key})))
    (assoc header
           :data image-data
           :stored-length stored-len
           :checksum checksum)))

(defn read-image-from-channel [^FileChannel channel size]
  {:pre [(pos? size)
         (>= (.size channel) (+ (.position channel) size))]}
  (let [buffer (io/read-from-channel channel size)]
    (read-image-from-buffer buffer)))

(defn read-image-from-file [source offset size]
  {:pre [(pos? size)]}
  (with-open [ra-file (RandomAccessFile. source "r")]
    (assert (<= (+ offset size) (.length ra-file)))
    (.seek ra-file offset)
    (let [channel (.getChannel ra-file)]
      (read-image-from-channel channel size))))