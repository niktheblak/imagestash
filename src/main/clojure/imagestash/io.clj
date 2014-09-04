(ns imagestash.io
  (:import [java.io InputStream]
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
  (.getBytes str default-charset))

(defn int-to-bytes [n]
  (let [i (int n)
        buffer (ByteBuffer/allocate 4)
        _ (.putInt buffer i)]
    (.array buffer)))

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
