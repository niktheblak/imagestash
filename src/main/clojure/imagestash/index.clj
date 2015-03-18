(ns imagestash.index
  (:import [java.io DataOutputStream DataInputStream]
           [clojure.lang Keyword]
           [imagestash.j FileChannels])
  (:require [imagestash.stash-core :as stash]
            [imagestash.image-io :as iio]
            [clojure.java.io :as jio]))

(defrecord IndexKey [^String key ^Long size ^Keyword format])

(defrecord IndexValue [^Long offset ^Long size])

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
    (let [amount (.readInt input)]
      (loop [i 0
             index (transient {})]
        (if (< i amount)
          (let [[k v] (read-index-item input)]
            (recur (inc i) (assoc! index k v)))
          (persistent! index))))))

(defn reconstruct-index [source]
  (with-open [channel (FileChannels/read source)]
    (loop [position 0
           images (transient {})]
      (if (< position (.size channel))
        (let [image (iio/read-image-from-channel-without-size channel position)
              k (IndexKey. (:key image) (:size image) (:format image))
              v (IndexValue. position (:storage-size image))]
          (recur (+ position (:storage-size image)) (assoc! images k v)))
        (persistent! images)))))
