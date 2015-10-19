(ns imagestash.str-util
  (:import [java.nio.charset Charset]))

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

(defn format-bytes [arr]
  {:post [(string? %)]}
  (if (coll? arr)
    (apply str (map #(hexify %) arr))
    (hexify arr)))
