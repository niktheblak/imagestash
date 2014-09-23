(ns imagestash.io
  (:import [java.io InputStream DataInput DataOutput]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.nio ByteBuffer]))

(def default-charset (Charset/forName "UTF-8"))

(defn byte-array? [arr]
  (let [c (class arr)]
    (and
      (.isArray c)
      (identical? (.getComponentType c) Byte/TYPE))))

(defn str-to-bytes [str]
  {:pre [(string? str)]}
  (.getBytes str default-charset))

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