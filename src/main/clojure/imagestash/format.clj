(ns imagestash.format
  (:require [clojure.string :as string]))

(def supported-formats #{:jpeg :png :gif})

(defn to-format [format]
  (if (contains? supported-formats format)
    format
    (let [format-str (string/upper-case format)]
      (case format-str
        "JPEG" :jpeg
        "JPG" :jpeg
        "PNG" :png
        "GIF" :gif))))

(defn to-string [format]
  (if (string? format)
    format
    (case format
      :jpeg "JPEG"
      :png "PNG"
      :gif "GIF")))

(defn supported-format? [format]
  {:pre [(keyword? format)]}
  (contains? supported-formats format))