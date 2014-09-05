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

(defmacro with-image [bindings & body]
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-image ~(subvec bindings 2) ~@body)
                                (finally
                                  (.flush ~(bindings 0)))))
    :else (throw (IllegalArgumentException.
                   "with-image only allows Symbols in bindings"))))

(defn- read-from [source]
  {:pre [(supported-source? source)]}
  (let [src (if (string? source) (URL. source) source)]
    (ImageIO/read src)))

(defn resize-image [source size format]
  {:pre [(number? size)
         (format/supported-format? format)]}
  (with-image [image (read-from source)
               resized (Scalr/resize image (int size))]
    (let [output (ByteArrayOutputStream.)
          format-str (format/to-string format)]
      (ImageIO/write resized format-str output)
      (.toByteArray output))))