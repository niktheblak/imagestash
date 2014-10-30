(ns imagestash.stash-io
  (:import [java.io RandomAccessFile]
           [java.util Arrays])
  (:require [clojure.set :as set]
            [imagestash.stash-core :refer :all]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(defn- write-header-to-file [^RandomAccessFile file digest {:keys [flags key size format data] :or {flags 0}}]
  (let [key-bytes (.getBytes key charset)
        format-code (format-to-code (format/parse-format format))]
  (doto file
    (io/write-bytes header-bytes)
    (io/write-byte flags)
    (io/write-short (alength key-bytes))
    (io/write-bytes key-bytes)
    (io/write-short size)
    (io/write-byte format-code)
    (io/write-int (alength data)))
  (d/update-digest digest header-bytes flags (alength key-bytes) key-bytes size format-code (alength data))))

(defn- write-data-to-file [^RandomAccessFile file digest data]
  (io/write-bytes file data)
  (d/update-digest digest data))

(defn- write-to-file [^RandomAccessFile file {:keys [flags key size format data]
                                                :or {flags 0} :as header}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (let [digest (d/new-digest)
        original-length (.length file)]
    (doto file
      (.seek original-length)
      (write-header-to-file digest header)
      (write-data-to-file digest data)
      (io/write-bytes (d/get-digest digest))
      (write-padding))
    (let [storage-size (- (.getFilePointer file) original-length)]
      {:offset original-length
       :size storage-size
       :checksum (d/get-digest digest)})))

(defn write-image-to-file [target {:keys [flags key size format data]
                        :or {flags 0} :as image}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [ra-file (RandomAccessFile. target "rw")]
    (write-to-file ra-file image)))

(defn- read-header [^RandomAccessFile ra-file & {:keys [digest]}]
  (let [header-on-disk (io/read-bytes (alength header-bytes) ra-file)]
    (when-not (Arrays/equals header-bytes header-on-disk)
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

(defn read-image-from-random-access-file [^RandomAccessFile ra-file]
  (let [digest (d/new-digest)
        original-pos (.getFilePointer ra-file)
        header (read-header ra-file :digest digest)
        image-data (io/read-bytes (:data-length header) ra-file)
        checksum (io/read-bytes d/digest-length ra-file)
        padding-len (padding-length (.getFilePointer ra-file))
        _ (.skipBytes ra-file (int padding-len))
        stored-len (- (.getFilePointer ra-file) original-pos)
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

(defn read-image-from-file [source offset]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (read-image-from-random-access-file ra-file)))