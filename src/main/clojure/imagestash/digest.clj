(ns imagestash.digest
  (:import [java.security MessageDigest])
  (:require [imagestash.io :as io]))

(defn get-digest []
  (MessageDigest/getInstance "MD5"))

(def digest-length (.getDigestLength (get-digest)))

(defn digest [& items]
  {:post [(io/byte-array? %)]}
  (let [digest (get-digest)]
    (doseq [n items]
      (cond
        (and (integer? n) (< n 256)) (.update digest (unchecked-byte n))
        (integer? n) (io/update-digest-int digest n)
        :else (.update digest (io/to-bytes n))))
    (.digest digest)))