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

(ns imagestash.resize
  (:import [javax.imageio ImageIO]
           [java.awt.image BufferedImage BufferedImageOp]
           [java.io ByteArrayOutputStream File]
           [java.net URL URI MalformedURLException]
           [org.imgscalr Scalr])
  (:require [imagestash.format :as format]
            [imagestash.io :as io]
            [imagestash.url :as url]))

(defmacro with-image [bindings & body]
  (assert (vector? bindings))
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-image ~(subvec bindings 2) ~@body)
                                (finally
                                  (.flush ~(bindings 0)))))
    :else (throw (IllegalArgumentException.
                   "with-image only allows Symbols in bindings"))))

(defmulti to-readable-source class)
(defmethod to-readable-source URL [source] source)
(defmethod to-readable-source URI [source] (.toURL source))
(defmethod to-readable-source File [source] source)
(defmethod to-readable-source String [source]
  (if-let [url (url/to-url source)]
    url
    (if-let [file (io/to-file source)]
      file
      nil)))
(defmethod to-readable-source :default [x] nil)

(defn- read-from [source]
  (if-let [src (to-readable-source source)]
    (ImageIO/read src)
    (throw (ex-info "Unsupported source" {:source source}))))

(defn supported-source? [source]
  (to-readable-source source))

(defn- resize [^BufferedImage image size]
  (Scalr/resize image (int size) (into-array BufferedImageOp [])))

(defn resize-image [source size format]
  {:pre [(number? size)
         (format/supported-format? format)]}
  (with-image [image (read-from source)
               resized (resize image size)]
    (let [output (ByteArrayOutputStream.)
          format-str (format/format-name format)]
      ; TODO: Add support for writing directly to OutputStream/ByteBuffer
      (ImageIO/write resized format-str output)
      (.toByteArray output))))
