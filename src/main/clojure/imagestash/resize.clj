(ns imagestash.resize
  (:import [javax.imageio ImageIO]
           [imagestash.j Scalr]
           [java.awt.image BufferedImage]
           [java.io ByteArrayOutputStream File]
           [java.net URL])
  (:require [imagestash.format :as format]))

(defn supported-source? [source]
  (or (instance? URL source)
      (instance? File source)
      (string? source)))

(defn with-image [source f]
  {:pre [(supported-source? source)]}
  (let [src (if (string? source) (URL. source) source)
        image (atom nil)]
    (try
      (reset! image (ImageIO/read src))
      (f @image)
      (finally
        (when @image
          (.flush @image))))))

(defn resize-image [source size format]
  {:pre [(number? size)
         (format/supported-format? format)]}
  (let [resized (with-image source (fn [^BufferedImage image] (Scalr/resize image (int size))))
        output (ByteArrayOutputStream.)
        format-str (format/to-string format)
        _ (ImageIO/write resized format-str output)]
    (.toByteArray output)))