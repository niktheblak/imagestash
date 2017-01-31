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

(ns imagestash.url
  (:import [java.net URL URI MalformedURLException]))

(defn java-url? [source]
  (instance? URL source))

(defn java-uri? [source]
  (instance? URI source))

(defn java-url-like? [source]
  (or
    (java-url? source)
    (java-uri? source)))

(defn parses-to-url? [source]
  (try
    (URL. source)
    true
    (catch MalformedURLException e false)))

(defn to-url [source]
  (cond
    (java-url? source) source
    (java-uri? source) (.toURL source)
    (and
      (string? source)
      (parses-to-url? source)) (URL. source)
    :else nil))

(defn url? [source]
  (cond
    (java-url-like? source) true
    (and
      (string? source)
      (not (.isEmpty source))) (parses-to-url? source)
    :else false))
