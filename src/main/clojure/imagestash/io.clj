(ns imagestash.io
  (:import [java.io InputStream DataInput DataOutput]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.nio ByteBuffer]
           [java.security MessageDigest]))

(def default-charset (Charset/forName "UTF-8"))

(defn byte-array? [arr]
  (let [c (class arr)]
    (and
      (.isArray c)
      (identical? (.getComponentType c) Byte/TYPE))))

(defn str-to-bytes [str]
  {:pre [(string? str)]}
  (.getBytes str default-charset))

(defn update-digest-int [digest n]
  (let [b4 (bit-and n 0xFF)
        b3 (bit-and (bit-shift-right n 8) 0xFF)
        b2 (bit-and (bit-shift-right n 16) 0xFF)
        b1 (bit-and (bit-shift-right n 24) 0xFF)]
    (doto digest
      (.update (unchecked-byte b1))
      (.update (unchecked-byte b2))
      (.update (unchecked-byte b3))
      (.update (unchecked-byte b4)))))

(defn to-bytes [n]
  {:post [(byte-array? %)]}
  (cond
    (byte-array? n) n
    (string? n) (str-to-bytes n)
    :else (byte-array (map unchecked-byte n))))

(defn read-fully [^InputStream input & {:keys [buffer-size] :or {buffer-size 4096}}]
  (loop [buffer (byte-array buffer-size)
         pos 0]
    (let [buf (if (= pos (alength buffer))
                (Arrays/copyOf buffer (* 2 (alength buffer)))
                buffer)
          b (.read input)]
      (if (not= b -1)
        (do
          (aset buf pos (unchecked-byte b))
          (recur buf (inc pos)))
        (Arrays/copyOfRange buf 0 pos)))))

(defn read-bytes [^DataInput input n]
  (let [buf (byte-array n)]
    (.readFully input buf)
    buf))

(defn read-and-digest-byte [^DataInput input ^MessageDigest digest]
  (let [b (.readByte input)]
    (.update digest b)
    b))

(defn read-and-digest-bytes [^DataInput input ^MessageDigest digest length]
  (let [buffer (byte-array length)]
    (.readFully input buffer)
    (.update digest buffer)
    buffer))

(defn read-and-digest-int [^DataInput input ^MessageDigest digest]
  (let [n (.readInt input)]
    (update-digest-int digest n)
    n))

(defn read-and-digest-short [^DataInput input ^MessageDigest digest]
  (let [n (.readUnsignedShort input)]
    (update-digest-int digest n)
    n))

(defn write-and-digest-byte [^DataOutput output ^MessageDigest digest b]
  (.writeByte output (int b))
  (.update digest (unchecked-byte b)))

(defn write-and-digest-bytes [^DataOutput output ^MessageDigest digest arr]
  (.write output arr)
  (.update digest arr))

(defn write-and-digest-int [^DataOutput output ^MessageDigest digest n]
  (.writeInt output (int n))
  (update-digest-int digest n))

(defn write-and-digest-short [^DataOutput output ^MessageDigest digest n]
  (.writeShort output (int n))
  (update-digest-int digest n))