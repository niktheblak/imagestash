(ns imagestash.digest
  (:import [java.security MessageDigest])
  (:require [imagestash.io :as io]))

(defn get-digest []
  (MessageDigest/getInstance "SHA-256"))

(defn digest [& items]
  {:post [(io/byte-array? %)]}
  (let [digest (get-digest)]
    (doseq [n items]
      (let [bytes (cond
                    (and (integer? n) (< n 256)) (byte-array [(unchecked-byte n)])
                    (integer? n) (io/int-to-bytes n)
                    :else (io/to-bytes n))]
        (.update digest bytes)))
    (.digest digest)))