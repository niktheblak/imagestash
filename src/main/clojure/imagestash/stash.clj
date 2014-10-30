(ns imagestash.stash
  (:import [imagestash.j RandomAccessFileInputStream]
           [java.io RandomAccessFile ByteArrayInputStream]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.security MessageDigest]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel])
  (:require [clojure.set :as set]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(def charset (Charset/forName "US-ASCII"))

(def header (.getBytes "IMG1" charset))

(def format-codes {:jpeg 0
                   :png  1
                   :gif  2})

(defn format-to-code [format]
  {:post [%]}
  (get format-codes format))

(defn code-to-format [format-code]
  {:post [%]}
  (let [code-formats (set/map-invert format-codes)]
    (get code-formats format-code)))

(defn- padding-length [position]
  {:pre [(pos? position)]
   :post [(>= % 0)
          (<= % 8)]}
  (let [offset (mod position 8)
        padding-length (if (= 0 offset)
                         0
                         (- 8 offset))]
    padding-length))

(defn size-on-disk [{:keys [flags key data]
                     :or   {flags 0}}]
  (let [len (+ (alength header)                             ; header length
               1                                            ; flags
               4                                            ; key length
               (alength (.getBytes key charset))            ; key data
               4                                            ; image size
               1                                            ; format
               (alength data)                               ; image data
               d/digest-length                              ; digest length
               )]
    (+ len (padding-length len))))

(defn- write-padding [^RandomAccessFile file]
  (let [pos (.getFilePointer file)
        len (padding-length pos)]
    (dotimes [i len]
      (.write file 0))))

(defn write-to-file [^RandomAccessFile file {:keys [flags key size format data]
                        :or {flags 0}}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
    (let [original-length (.length file)
          key-bytes (.getBytes key charset)
          format-code (format-to-code (format/parse-format format))
          checksum (d/digest header flags (alength key-bytes) key-bytes size format-code (alength data) data)]
      (doto file
        (.seek original-length)
        (io/write-bytes header)
        (io/write-byte flags)
        (io/write-short (alength key-bytes))
        (io/write-bytes key-bytes)
        (io/write-short size)
        (io/write-byte format-code)
        (io/write-int (alength data))
        (io/write-bytes data)
        (io/write-bytes checksum)
        (write-padding))
      (let [storage-size (- (.getFilePointer file) original-length)]
        {:offset original-length
         :size storage-size
         :checksum checksum})))

(defn write-to [target {:keys [flags key size format data]
                        :or {flags 0} :as image}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [ra-file (RandomAccessFile. target "rw")]
    (write-to-file ra-file image)))

(defn read-header [^RandomAccessFile ra-file & {:keys [digest]}]
  (let [header-bytes (io/read-bytes (alength header) ra-file)]
    (when-not (Arrays/equals header header-bytes)
      (throw (ex-info "Invalid header" {:header header-bytes})))
    (let [flags (io/read-byte ra-file)
          key-len (io/read-short ra-file)
          key-buf (io/read-bytes key-len ra-file)
          image-key (String. key-buf charset)
          image-size (io/read-short ra-file)
          format-code (io/read-byte ra-file)
          image-format (code-to-format format-code)
          data-len (io/read-int ra-file)]
      (when digest
        (d/update-digest digest header-bytes flags key-len key-buf image-size format-code data-len))
      {:flags       flags
       :key         image-key
       :size        image-size
       :format      image-format
       :data-length data-len})))

(defn read-from-file [^RandomAccessFile ra-file]
  (let [digest (d/get-digest)
        original-pos (.getFilePointer ra-file)
        header (read-header ra-file :digest digest)
        image-data (io/read-bytes (:data-length header) ra-file)
        checksum (io/read-bytes d/digest-length ra-file)
        padding-len (padding-length (.getFilePointer ra-file))
        _ (.skipBytes ra-file (int padding-len))
        stored-len (- (.getFilePointer ra-file) original-pos)
        _ (d/update-digest digest image-data)
        expected-checksum (.digest digest)]
    (if (Arrays/equals expected-checksum checksum)
      (assoc header
        :data image-data
        :stored-length stored-len
        :checksum checksum)
      (throw (ex-info
               "Image checksum does not match"
               {:key    key
                :offset original-pos})))))

(defn read-from [source offset]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (read-from-file ra-file)))

(defn read-header-from-buffer [^ByteBuffer buffer & {:keys [digest]}]
  (let [header-bytes (io/read-bytes-from-buffer buffer (alength header))]
    (when-not (Arrays/equals header header-bytes)
      (throw (ex-info "Invalid header" {:header header-bytes})))
    (let [flags (.get buffer)
          key-len (.getShort buffer)
          key-buf (io/read-bytes-from-buffer buffer key-len)
          image-key (String. key-buf charset)
          image-size (.getShort buffer)
          format-code (.get buffer)
          image-format (code-to-format format-code)
          data-len (.getInt buffer)]
      (when digest
        (d/update-digest digest header-bytes flags key-len key-buf image-size format-code data-len))
      {:flags       flags
       :key         image-key
       :size        image-size
       :format      image-format
       :data-length data-len})))

(defn- read-data-from-channel [^FileChannel channel size]
  (let [buffer (ByteBuffer/allocate size)]
    (.read channel buffer)
    (.rewind buffer)
    buffer))

(defn read-from-channel [^FileChannel channel size]
  {:pre [(pos? size)
         (>= (.size channel) (+ (.position channel) size))]}
  (let [digest (d/get-digest)
        original-pos (.position channel)
        buffer (read-data-from-channel channel size)
        header (read-header-from-buffer buffer :digest digest)
        image-data (io/read-bytes-from-buffer buffer (:data-length header))
        checksum (io/read-bytes-from-buffer buffer d/digest-length)
        padding-len (padding-length (.position channel))
        _ (io/skip-buffer buffer (int padding-len))
        stored-len (- (.position channel) original-pos)
        _ (d/update-digest digest image-data)
        expected-checksum (.digest digest)]
    (if (Arrays/equals expected-checksum checksum)
      (assoc header
             :data image-data
             :stored-length stored-len
             :checksum checksum)
      (throw (ex-info
               "Image checksum does not match"
               {:key    key
                :offset original-pos})))))

(defn get-image-stream [source offset size]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (let [channel (.getChannel ra-file)
          image (read-from-channel channel size)
          stream (ByteArrayInputStream. (:data image))]
      (assoc image :stream stream))))