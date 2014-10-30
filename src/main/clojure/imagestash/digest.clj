(ns imagestash.digest
  (:import [java.security MessageDigest]
           [java.io DataInput DataOutput])
  (:require [imagestash.io :as io]))

(defn new-digest []
  (MessageDigest/getInstance "MD5"))

(defn get-digest [^MessageDigest digest]
  (.digest digest))

(def digest-length (.getDigestLength (new-digest)))

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

(defn update-digest [^MessageDigest digest & items]
  (doseq [data items]
    (cond
      (and (integer? data) (<= data 255)) (.update digest (unchecked-byte data))
      (integer? data) (update-digest-int digest data)
      :else (.update digest (io/to-bytes data)))))

(defn write-and-digest [^DataOutput output ^MessageDigest digest data write-fn]
  (write-fn output data)
  (update-digest digest data))

(defn digest [& items]
  {:post [(io/byte-array? %)]}
  (let [digest (new-digest)]
    (doseq [item items]
      (update-digest digest item))
    (.digest digest)))