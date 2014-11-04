(ns imagestash.stash-io
  (:import [java.io RandomAccessFile]
           [java.util Arrays])
  (:require [imagestash.stash-core :refer :all]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

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

(defn read-image-from-file [^RandomAccessFile ra-file]
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
    (when-not (Arrays/equals expected-checksum checksum)
      (throw (ex-info
               "Image checksum does not match"
               {:key    key
                :offset original-pos})))
    (assoc header
           :data image-data
           :stored-length stored-len
           :checksum checksum)))