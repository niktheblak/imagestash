(ns imagestash.stash
  (:import [java.io RandomAccessFile]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.security MessageDigest])
  (:require [clojure.set :as set]
            [imagestash.format :as format]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(def charset (Charset/forName "US-ASCII"))

(def header (.getBytes "IMG1" charset))

(def format-codes {:jpeg 0
                   :png  1
                   :gif  2})

(defn- format-to-code [format]
  (let [code (get format-codes format)]
    (if (nil? code)
      (throw (ex-info "Invalid format" {:format format}))
      code)))

(defn- code-to-format [format-code]
  (let [code-formats (set/map-invert format-codes)
        format (get code-formats format-code)]
    (if (nil? format)
      (throw (ex-info "Invalid format" {:format format}))
      format)))

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
    (let [digest (d/get-digest)
          original-length (.length file)
          key-bytes (.getBytes key charset)
          format-code (format-to-code (format/to-format format))]
      (doto file
        (.seek original-length)
        (io/write-and-digest-bytes digest header)
        (io/write-and-digest-byte digest flags)
        (io/write-and-digest-short digest (alength key-bytes))
        (io/write-and-digest-bytes digest key-bytes)
        (io/write-and-digest-short digest (int size))
        (io/write-and-digest-byte digest format-code)
        (io/write-and-digest-int digest (alength data))
        (io/write-and-digest-bytes digest data)
        (.write (.digest digest))
        (write-padding))
      (let [storage-size (- (.getFilePointer file) original-length)]
        {:offset original-length
         :size storage-size
         :checksum (.digest digest)})))

(defn write-to [target {:keys [flags key size format data]
                        :or {flags 0} :as image}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [ra-file (RandomAccessFile. target "rw")]
    (write-to-file ra-file image)))

(defn read-from-file [^RandomAccessFile ra-file]
  (let [digest (d/get-digest)
        original-pos (.getFilePointer ra-file)
        header-bytes (io/read-and-digest-bytes ra-file digest (alength header))]
    (when-not (Arrays/equals header header-bytes)
      (throw (ex-info "Invalid header" {:header header-bytes})))
    (let [flags (io/read-and-digest-byte ra-file digest)
          key-len (io/read-and-digest-short ra-file digest)
          key-buf (io/read-and-digest-bytes ra-file digest key-len)
          image-key (String. key-buf charset)
          image-size (io/read-and-digest-short ra-file digest)
          format-code (io/read-and-digest-byte ra-file digest)
          image-format (code-to-format format-code)
          data-len (io/read-and-digest-int ra-file digest)
          image-data (io/read-and-digest-bytes ra-file digest data-len)
          digest-bytes (io/read-bytes ra-file d/digest-length)
          padding-len (padding-length (.getFilePointer ra-file))
          _ (.skipBytes ra-file (int padding-len))
          stored-len (- (.getFilePointer ra-file) original-pos)
          expected-digest-bytes (.digest digest)]
      (if (Arrays/equals expected-digest-bytes digest-bytes)
        {:flags         flags
         :key           image-key
         :size          image-size
         :format        image-format
         :data          image-data
         :stored-length stored-len
         :checksum      digest-bytes}
        (throw (ex-info
                 "Image data does not match checksum"
                 {:key image-key
                  :offset original-pos}))))))

(defn read-from [source offset]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (read-from-file ra-file)))