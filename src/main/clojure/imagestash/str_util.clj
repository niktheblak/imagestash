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

(ns imagestash.str-util
  (:import [java.nio.charset Charset])
  (:require [imagestash.types :refer :all]))

(def default-charset (Charset/forName "UTF-8"))

(defn to-bytes [^String str]
  (.getBytes str default-charset))

(defn repeat-str [^long n ^String s]
  {:pre  [(pos? n)
          (pos? (.length s))]
   :post [(= (* n (.length s) (.length %)))]}
  (let [builder (StringBuilder. (* n (.length s)))]
    (dotimes [_ n]
      (.append builder s))
    (.toString builder)))

(defn pad [^String s ^long n c]
  {:pre  [(pos? n)]
   :post [(>= (.length %) n)]}
  (if (>= (.length s) n)
    s
    (let [amount (- n (.length s))
          padding-str (str c)
          padding-len (.length padding-str)
          padding (if (> amount padding-len)
                    (let [n (inc (quot amount padding-len))
                          p (repeat-str n padding-str)]
                      (.substring p 0 amount))
                    (.substring padding-str 0 amount))]
      (str padding s))))

(defn hexify [n]
  (pad (Long/toHexString (long n)) 2 \0))

(defn- hex-stringify [arr]
  (apply str (map #(hexify %) arr)))

(defn- long? [n]
  (and
    (integer? n)
    (>= n Long/MIN_VALUE)
    (<= n Long/MAX_VALUE)))

(defn format-bytes [arr]
  {:post [(string? %)]}
  (cond
    (byte-array? arr) (hex-stringify arr)
    (coll? arr) (hex-stringify arr)
    (long? arr) (hexify arr)
    (number? arr) (hex-stringify (.toByteArray (biginteger arr)))
    :else (throw (IllegalArgumentException. (str "Cannot format value " arr)))))