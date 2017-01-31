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

(ns imagestash.flags)

(def supported-flags [:deleted])

(defn encode-set [flag-values flags]
  {:pre [(sequential? flag-values)
         (not-empty flag-values)
         (< (count flag-values) Long/SIZE)
         (coll? flags)]}
  (reduce
    (fn [n f]
      (let [index (.indexOf flag-values f)]
        (when (= -1 index)
          (throw (IllegalArgumentException. (str "flag-values does not contain value " f))))
        (bit-set n index)))
    0
    flags))

(defn encode-flags [flags]
  (unchecked-byte (encode-set supported-flags flags)))

(defn- bits-set [n]
  (- Long/SIZE (Long/numberOfLeadingZeros n)))

(defn- validate-flags [flag-values flags]
  (when (> (bits-set flags) (count flag-values))
    (throw (IllegalArgumentException.
             (str
               "flags contains more set bits ("
               (Long/toBinaryString flags)
               ") than flag-values has members ("
               flags
               ")")))))

(defn decode-set [flag-values flags]
  {:pre  [(sequential? flag-values)
          (integer? flags)]
   :post [(set? %)]}
  (validate-flags flag-values flags)
  (let [limit (bits-set flags)]
    (reduce
      (fn [fs n]
        (if (bit-test flags n)
          (conj fs (nth flag-values n))
          fs))
      #{}
      (range limit))))

(defn decode-flags [flags]
  (decode-set supported-flags flags))
