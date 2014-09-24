(ns imagestash.digest
  (:import [java.security MessageDigest]
           [java.io DataInput DataOutput])
  (:require [imagestash.io :as io]))

(defn get-digest []
  (MessageDigest/getInstance "MD5"))

(def digest-length (.getDigestLength (get-digest)))

(defn update-digest-int [^MessageDigest digest n]
  (let [b4 (bit-and n 0xFF)
        b3 (bit-and (bit-shift-right n 8) 0xFF)
        b2 (bit-and (bit-shift-right n 16) 0xFF)
        b1 (bit-and (bit-shift-right n 24) 0xFF)]
    (doto digest
      (.update (unchecked-byte b1))
      (.update (unchecked-byte b2))
      (.update (unchecked-byte b3))
      (.update (unchecked-byte b4)))))

(defn update-digest [^MessageDigest digest data]
  (cond
    (and (integer? data) (<= data 255)) (.update digest (unchecked-byte data))
    (integer? data) (update-digest-int digest data)
    (io/byte-array? data) (.update digest data)
    :else (.update digest (io/to-bytes data))))

(defn read-and-digest [^DataInput input ^MessageDigest digest read-fn]
  (let [data (read-fn input)]
    (when digest
      (update-digest digest data))
    data))

(defn write-and-digest [^DataOutput output ^MessageDigest digest data write-fn]
  (write-fn output data)
  (update-digest digest data))

(defn digest [& items]
  {:post [(io/byte-array? %)]}
  (let [digest (get-digest)]
    (doseq [n items]
      (update-digest digest n))
    (.digest digest)))