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

(defn read-byte [^DataInput input]
  (.readByte input))

(defn read-short [^DataInput input]
  (.readUnsignedShort input))

(defn read-int [^DataInput input]
  (.readInt input))

(defn read-bytes [n ^DataInput input]
  (let [buf (byte-array n)]
    (.readFully input buf)
    buf))

(defn write-byte [^DataOutput output n]
  (.writeByte output n))

(defn write-bytes [^DataOutput output data]
  (.write output data))

(defn write-short [^DataOutput output n]
  (.writeShort output n))

(defn write-int [^DataOutput output n]
  (.writeInt output n))

(defn update-digest [^MessageDigest digest data]
  (cond
    (instance? Byte data) (.update digest data)
    (integer? data) (update-digest-int digest data)
    (byte-array? data) (.update digest data)
    :else (throw (ex-info "Unknown data format" {:data data}))))

(defn read-and-digest [^DataInput input ^MessageDigest digest read-fn]
  (let [data (read-fn input)]
    (update-digest digest data)
    data))

(defn write-and-digest [^DataOutput output ^MessageDigest digest data write-fn]
  (write-fn output data)
  (update-digest digest data))