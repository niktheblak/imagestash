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
