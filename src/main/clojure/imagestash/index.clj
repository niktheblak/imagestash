(ns imagestash.index
  (:import [java.io File RandomAccessFile DataOutputStream DataInputStream]
           [clojure.lang Keyword])
  (:require [imagestash.stash-core :as stash]
            [imagestash.stash-io :as sio]
            [clojure.java.io :as jio]))

(defrecord IndexKey [^String key ^Long size ^Keyword format])

(defrecord IndexValue [^Long offset ^Long size])

(defn index-key [{:keys [key size format]}]
  (str key size format))

(defn- read-index-item [^DataInputStream input]
  (let [key (.readUTF input)
        image-size (.readInt input)
        format-code (.readByte input)
        format (stash/code-to-format format-code)
        offset (.readLong input)
        size (.readLong input)
        k (IndexKey. key image-size format)
        v (IndexValue. offset size)]
    [k v]))

(defn- write-index-item [^DataOutputStream output k v]
  (doto output
    (.writeUTF (:key k))
    (.writeInt (int (:size k)))
    (.write (stash/format-to-code (:format k)))
    (.writeLong (:offset v))
    (.writeLong (:size v))))

(defn save-index [target index]
  (with-open [output (DataOutputStream. (jio/output-stream target))]
    (.writeInt output (count index))
    (doseq [[k v] (seq index)]
      (write-index-item output k v))))

(defn load-index [source]
  (with-open [input (DataInputStream. (jio/input-stream source))]
    (let [index (atom {})
          amount (.readInt input)]
      (dotimes [i amount]
        (let [[k v] (read-index-item input)]
          (swap! index assoc k v)))
      @index)))

(defn reconstruct-index [source]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (loop [bytes-read 0
           images {}]
      (let [current-offset (.getFilePointer ra-file)
            image (sio/read-image-from-file ra-file)
            new-bytes-read (+ bytes-read (:stored-length image))
            index-key (IndexKey. (:key image) (:size image) (:format image))
            value (IndexValue. current-offset (:stored-length image))
            new-images (assoc images index-key value)]
        (if (< new-bytes-read (.length ra-file))
          (recur new-bytes-read new-images)
          new-images)))))