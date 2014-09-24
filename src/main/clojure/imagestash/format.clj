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