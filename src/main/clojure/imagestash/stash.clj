(ns imagestash.stash
  (:import [imagestash.j ImageInputStream]
           [java.io RandomAccessFile]
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
          format-code (format-to-code (format/parse-format format))]
      (doto file
        (.seek original-length)
        (d/write-and-digest digest header io/write-bytes)
        (d/write-and-digest digest (unchecked-byte flags) io/write-byte)
        (d/write-and-digest digest (alength key-bytes) io/write-short)
        (d/write-and-digest digest key-bytes io/write-bytes)
        (d/write-and-digest digest (int size) io/write-short)
        (d/write-and-digest digest (unchecked-byte format-code) io/write-byte)
        (d/write-and-digest digest (alength data) io/write-int)
        (d/write-and-digest digest data io/write-bytes)
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

(defn read-header [^RandomAccessFile ra-file & {:keys [digest]}]
  (let [header-bytes (d/read-and-digest ra-file digest (partial io/read-bytes (alength header)))]
    (when-not (Arrays/equals header header-bytes)
      (throw (ex-info "Invalid header" {:header header-bytes})))
    (let [flags (d/read-and-digest ra-file digest io/read-byte)
          key-len (d/read-and-digest ra-file digest io/read-short)
          key-buf (d/read-and-digest ra-file digest (partial io/read-bytes key-len))
          image-key (String. key-buf charset)
          image-size (d/read-and-digest ra-file digest io/read-short)
          format-code (d/read-and-digest ra-file digest io/read-byte)
          image-format (code-to-format format-code)
          data-len (d/read-and-digest ra-file digest io/read-int)]
      {:flags       flags
       :key         image-key
       :size        image-size
       :format      image-format
       :data-length data-len})))

(defn read-from-file [^RandomAccessFile ra-file]
  (let [digest (d/get-digest)
        original-pos (.getFilePointer ra-file)
        header (read-header ra-file :digest digest)
        image-data (d/read-and-digest ra-file digest (partial io/read-bytes (:data-length header)))
        digest-bytes (io/read-bytes d/digest-length ra-file)
        padding-len (padding-length (.getFilePointer ra-file))
        _ (.skipBytes ra-file (int padding-len))
        stored-len (- (.getFilePointer ra-file) original-pos)
        expected-digest-bytes (.digest digest)]
    (if (Arrays/equals expected-digest-bytes digest-bytes)
      (assoc header
        :data image-data
        :stored-length stored-len
        :checksum digest-bytes)
      (throw (ex-info
               "Image data does not match checksum"
               {:key    key
                :offset original-pos})))))

(defn read-from [source offset]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (read-from-file ra-file)))

(defn get-image-stream [source offset]
  (let [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (let [header (read-header ra-file)
          data-len (:data-length header)
          stream (ImageInputStream. ra-file data-len)]
      (assoc header :stream stream))))