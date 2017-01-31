; Copyright 2016 Niko Korhonen
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0

; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns imagestash.format
  (:require [clojure.string :as string]))

(def supported-formats #{:jpeg :png :gif})

(defn parse-format [format]
  (if (contains? supported-formats format)
    format
    (let [format-str (string/upper-case format)]
      (case format-str
        "JPEG" :jpeg
        "JPG" :jpeg
        "PNG" :png
        "GIF" :gif))))

(defn format-name [format]
  (if (string? format)
    format
    (case format
      :jpeg "JPEG"
      :png "PNG"
      :gif "GIF")))

(defn format-mime-type [format]
  (case format
    :jpeg "image/jpeg"
    :png "image/png"
    :gif "image/gif"))

(defn parse-format-from-mime-type [mime-type]
  (case mime-type
    "image/jpeg" :jpeg
    "image/png" :png
    "image/gif" :gif))

(defn supported-format? [format]
  {:pre [(keyword? format)]}
  (contains? supported-formats format))