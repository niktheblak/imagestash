(ns imagestash.stash
  (:import [java.io RandomAccessFile]
           [java.util Arrays]
           [java.nio.charset Charset])
  (:require [clojure.set :as set]
            [imagestash.format :as format]
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

(defn- padding-length [len]
  {:pre [(pos? len)]
   :post [(>= % 0)
          (<= % 8)]}
  (let [offset (mod len 8)
        padding-length (if (= 0 offset) 0 (- 8 offset))]
    padding-length))

(defn- get-padding-bytes [ra-file]
  (let [pos (.getFilePointer ra-file)
        len (padding-length pos)]
    (byte-array len)))

(defn size-on-disk [{:keys [flags key data]
                     :or   {flags 0}}]
  (let [len (+ (alength header)                             ; header length
               1                                            ; flags
               4                                            ; key length
               (alength (.getBytes key charset))            ; key data
               4                                            ; image size
               1                                            ; format
               (alength data)                               ; image data
               )]
    (+ len (padding-length len))))

(defn write-to [target {:keys [flags key size format data]
                      :or {flags 0}}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)
         (io/byte-array? data)]}
  (with-open [ra-file (RandomAccessFile. target "rw")]
    (let [original-length (.length ra-file)
          key-bytes (.getBytes key charset)
          format-code (format-to-code (format/to-format format))]
      (doto ra-file
        (.seek original-length)
        (.write header)
        (.write flags)
        (.writeInt (alength key-bytes))
        (.write key-bytes)
        (.writeInt (int size))
        (.write format-code)
        (.writeInt (alength data))
        (.write data)
        (.write (get-padding-bytes ra-file)))
      (let [size-on-disk (- (.getFilePointer ra-file) original-length)]
        {:offset original-length
         :size-on-disk size-on-disk}))))

(defn read-from-file [^RandomAccessFile ra-file]
  (let [start-offset (.getFilePointer ra-file)
        header-buf (byte-array (alength header))
        _ (.readFully ra-file header-buf)]
    (when-not (Arrays/equals header header-buf)
      (throw (ex-info "Invalid header" {:header header-buf})))
    (let [flags (.readByte ra-file)
          key-len (.readInt ra-file)
          key-buf (byte-array key-len)
          _ (.readFully ra-file key-buf)
          image-key (String. key-buf charset)
          image-size (.readInt ra-file)
          format-code (.readByte ra-file)
          image-format (code-to-format format-code)
          data-len (.readInt ra-file)
          image-data (byte-array data-len)
          _ (.readFully ra-file image-data)
          padding-len (padding-length (.getFilePointer ra-file))
          _ (.skipBytes ra-file (int padding-len))
          stored-len (- (.getFilePointer ra-file) start-offset)]
      {:flags         flags
       :key           image-key
       :size          image-size
       :format        image-format
       :data          image-data
       :stored-length stored-len})))

(defn read-from [source offset]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (.seek ra-file offset)
    (read-from-file ra-file)))