(ns imagestash.io
  (:import [java.io InputStream DataInput]
           [java.util Arrays]
           [java.nio.charset Charset]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel]))

(def default-charset (Charset/forName "UTF-8"))

(defn byte-array? [arr]
  (let [c (class arr)]
    (and
      (.isArray c)
      (identical? (.getComponentType c) Byte/TYPE))))

(defn str-to-bytes [str]
  {:pre [(string? str)]}
  (.getBytes str default-charset))

(defn to-bytes [input]
  {:post [(byte-array? %)]}
  (cond
    (byte-array? input) input
    (string? input) (str-to-bytes input)
    (coll? input) (byte-array (map unchecked-byte input))
    :else (throw (ex-info "Cannot convert input to bytes" {:input input}))))

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

(defn read-bytes-from-buffer [^ByteBuffer buffer n]
  (let [data (byte-array n)]
    (.get buffer data)
    data))

(defn skip-buffer [^ByteBuffer buffer n]
  (.position buffer (+ (.position buffer) n)))

(defn read-from-channel [^FileChannel channel position size]
  (let [buffer (ByteBuffer/allocate size)]
    (.read channel buffer position)
    (.rewind buffer)
    buffer))