(ns imagestash.digest
  (:import [java.security MessageDigest]
           [java.util Arrays]
           [java.nio ByteBuffer])
  (:require [imagestash.io :as io]
            [imagestash.str-util :as str]
            [imagestash.types :refer :all]))

(defn new-digest []
  (MessageDigest/getInstance "MD5"))

(defn get-digest [^MessageDigest digest]
  (.digest digest))

(def digest-length
  "16 bytes for MD5"
  (.getDigestLength (new-digest)))

(defn update-digest-int [^MessageDigest digest n]
  {:pre [(integer? n)
         (>= n Integer/MIN_VALUE)
         (<= n Integer/MAX_VALUE)]}
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
      (instance? ByteBuffer data) (.update digest data)
      :else (.update digest (io/to-bytes data)))))

(defn digest [& items]
  {:post [(byte-array? %)
          (= digest-length (alength %))]}
  (let [digest (new-digest)]
    (doseq [item items]
      (update-digest digest item))
    (.digest digest)))

(defn verify-checksum [expected-checksum actual-checksum]
  {:pre [(byte-array? expected-checksum)
         (byte-array? actual-checksum)
         (= digest-length (alength expected-checksum) (alength actual-checksum))]}
  (when-not (Arrays/equals expected-checksum actual-checksum)
    (throw (ex-info "Checksum does not match"
                    {:expected-checksum (str/format-bytes expected-checksum)
                     :actual-checksum   (str/format-bytes actual-checksum)}))))
