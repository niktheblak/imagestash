(ns imagestash.stash-core
  (:import [java.io RandomAccessFile]
           [java.nio.charset Charset])
  (:require [clojure.set :as set]
            [imagestash.digest :as d]
            [imagestash.io :as io]))

(def charset (Charset/forName "US-ASCII"))

(def header-bytes (.getBytes "IMG1" charset))

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

(defn padding-length [position]
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
  (let [len (+ (alength header-bytes)                       ; header length
               1                                            ; flags
               4                                            ; key length
               (alength (.getBytes key charset))            ; key data
               4                                            ; image size
               1                                            ; format
               (alength data)                               ; image data
               d/digest-length                              ; digest length
               )]
    (+ len (padding-length len))))

(defn write-padding [^RandomAccessFile file]
  (let [pos (.getFilePointer file)
        len (padding-length pos)]
    (dotimes [i len]
      (.write file 0))))
