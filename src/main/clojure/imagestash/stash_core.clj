(ns imagestash.stash-core
  (:import [java.nio.charset Charset])
  (:require [clojure.set :as set]
            [imagestash.digest :as d]))

(def charset (Charset/forName "US-ASCII"))

(def header-bytes (.getBytes "IMG1" charset))

(def header-bytes-length (alength header-bytes))

(def format-codes {:jpeg 0
                   :png  1
                   :gif  2})

(def code-formats (set/map-invert format-codes))

(defn format-to-code [format]
  {:post [%]}
  (get format-codes format))

(defn code-to-format [format-code]
  {:post [%]}
  (get code-formats format-code))

(defn padding-length [position]
  {:pre [(pos? position)]
   :post [(>= % 0)
          (<= % 8)]}
  (let [offset (mod position 8)
        padding-length (if (= 0 offset)
                         0
                         (- 8 offset))]
    padding-length))

(def up-to-key-length (+ header-bytes-length  ; header length
                         1                    ; flags
                         2))                  ; key length

(defn up-to-data-length [key-len]
  (+ key-len  ; key length
     2        ; image size
     1        ; format
     4))      ; image data length

(defn stored-image-size [{:keys [key data key-length data-length]}]
  {:pre [(or key (pos? key-length))
         (or data (pos? data-length))]}
  (let [key-len (if key-length
                  key-length
                  (alength (.getBytes key charset)))
        data-len (if data-length
                   data-length
                   (alength data))
        len (+ header-bytes-length ; header length
               1                   ; flags
               2                   ; key length
               key-len             ; key data
               2                   ; image size
               1                   ; format
               4                   ; image data length
               data-len            ; image data
               d/digest-length)]   ; digest length
    (+ len (padding-length len))))
