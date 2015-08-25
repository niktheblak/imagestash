(ns imagestash.str-util
  (:import [java.nio.charset Charset]))

(def default-charset (Charset/forName "UTF-8"))

(defn to-bytes [str]
  {:pre [(string? str)]}
  (.getBytes str default-charset))

(defn pad [s n c]
  (if (>= (.length s) n)
    s
    (let [amount (- n (.length s))
          padding (apply str (take amount (repeat c)))]
      (str padding s))))

(defn hexify [n]
  (pad (Integer/toHexString (Byte/toUnsignedInt n)) 2 \0))

(defn format-bytes [arr]
  (apply str (map #(hexify %) arr)))
