(ns imagestash.stash-core
  (:import [java.nio.charset Charset])
  (:require [clojure.set :as set]
            [imagestash.digest :as d]))

(def charset (Charset/forName "US-ASCII"))

(def preamble-bytes (.getBytes "IMG1" charset))

(def preamble-size (alength preamble-bytes))

(def header-size
  "Image record header size is 30 bytes"
  (+ preamble-size                                          ; preamble
     1                                                      ; flags
     2                                                      ; image resolution
     1                                                      ; image format
     2                                                      ; image key size
     4                                                      ; image data size
     d/digest-length))                                      ; checksum

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

(defn padding-amount [position]
  {:pre  [(>= position 0)]
   :post [(>= % 0)
          (<= % 8)]}
  (let [offset (mod position 8)
        padding (if (= 0 offset)
                  0
                  (- 8 offset))]
    padding))

; Stored image structure is:
;
; Size | Index | Description      | Segment
; -----|-------|------------------|--------
; 4    | 0-3   | preamble 'IMG1'  | \
; 1    | 4     | flags            | |
; 2    | 5-6   | image resolution | |
; 1    | 7     | image format     | |- Header 30 bytes
; 2    | 8-9   | image key size   | |
; 4    | 10-13 | image data size  | |
; 16   | 14-29 | checksum         | /
; k    | 30-   | image key        | \
; x    | ..    | image data       | |- Payload n bytes
; 0-7  | ..    | padding          | /

(defn stored-image-size [{:keys [key data key-length data-length]}]
  {:pre [(or key (pos? key-length))
         (or data (pos? data-length))]}
  (let [key-len (if key-length
                  key-length
                  (alength (.getBytes key charset)))
        data-len (if data-length
                   data-length
                   (alength data))
        len (+ preamble-size                                ; preamble
               1                                            ; flags
               2                                            ; image resolution
               1                                            ; image format
               2                                            ; image key size
               4                                            ; image data size
               d/digest-length                              ; checksum
               key-len                                      ; key data
               data-len)]                                   ; image data
    (+ len (padding-amount len))))
