(ns imagestash.flags)

(def supported-flags [:deleted])

(defn encode-set [flag-values flags]
  {:pre [(sequential? flag-values)
         (not-empty flag-values)
         (< (count flag-values) 64)
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

(defn decode-set [flag-values limit flags]
  {:pre [(sequential? flag-values)
         (pos? limit)
         (integer? flags)]
   :post [(set? %)]}
  (reduce
    (fn [fs n]
      (if (bit-test flags n)
        (conj fs (nth flag-values n))
        fs))
    #{}
    (range limit)))

(defn decode-flags [flags]
  (decode-set supported-flags 8 flags))
