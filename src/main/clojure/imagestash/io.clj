(ns imagestash.io
  (:import [java.io InputStream File]
           [java.util Arrays]
           [java.nio ByteBuffer]
           [java.nio.channels FileChannel])
  (:require [imagestash.str-util :as str]
            [imagestash.types :refer :all]))

(defn file-exists? [^File file]
  (and
    (.exists file)
    (.canRead file)
    (pos? (.length file))))

(defn to-file [source]
  (cond
    (and
      (instance? File source)
      (file-exists? source)) source
    (string? source)
      (let [file (File. source)]
        (if (file-exists? file)
          file
          nil))
    :else nil))

(defn to-bytes [input]
  {:post [(byte-array? %)]}
  (cond
    (byte-array? input) input
    (string? input) (str/to-bytes input)
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

(defn read-bytes-from-buffer [^ByteBuffer buffer amount]
  {:pre  [(>= (.remaining buffer) amount)]
   :post [(= (alength %) amount)]}
  (let [data (byte-array amount)]
    (.get buffer data)
    data))

(defn skip-buffer [^ByteBuffer buffer amount]
  {:pre [(>= (.remaining buffer) amount)]}
  (.position buffer (+ (.position buffer) amount)))

(defn read-from-channel [^FileChannel channel position amount]
  {:pre  [(>= (.size channel) (+ position amount))]
   :post [(= (.remaining %) amount)]}
  (let [buffer (ByteBuffer/allocate amount)
        bytes-read (.read channel buffer position)]
    (assert (= bytes-read amount) "Could not read requested amount of bytes")
    (.rewind buffer)
    buffer))
