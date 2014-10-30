(ns imagestash.stash-nio
  (:import [java.io RandomAccessFile ByteArrayInputStream]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel])
  (:require [clojure.set :as set]
            [imagestash.stash-core :refer :all]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(defn- read-header-from-buffer [^ByteBuffer buffer & {:keys [digest]}]
  (let [header-bytes (io/read-bytes-from-buffer buffer (alength header-bytes))]
    (when-not (Arrays/equals header-bytes header-bytes)
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

(defn read-image-from-channel [^FileChannel channel size]
  {:pre [(pos? size)
         (>= (.size channel) (+ (.position channel) size))]}
  (let [digest (d/new-digest)
        original-pos (.position channel)
        buffer (io/read-from-channel channel size)
        header (read-header-from-buffer buffer :digest digest)
        image-data (io/read-bytes-from-buffer buffer (:data-length header))
        checksum (io/read-bytes-from-buffer buffer d/digest-length)
        padding-len (padding-length (.position channel))
        _ (io/skip-buffer buffer (int padding-len))
        stored-len (- (.position channel) original-pos)
        _ (d/update-digest digest image-data)
        expected-checksum (d/get-digest digest)]
    (if (Arrays/equals expected-checksum checksum)
      (assoc header
             :data image-data
             :stored-length stored-len
             :checksum checksum)
      (throw (ex-info
               "Image checksum does not match"
               {:key    key
                :offset original-pos})))))

(defn read-image-from-file [source offset size]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (let [channel (.getChannel ra-file)
          image (read-image-from-channel channel size)
          stream (ByteArrayInputStream. (:data image))]
      (assoc image :stream stream))))
