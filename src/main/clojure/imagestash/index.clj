(ns imagestash.index
  (:import [java.io DataOutputStream DataInputStream]
           [clojure.lang Keyword]
           [imagestash.j FileChannels])
  (:require [imagestash.stash-core :as stash]
            [imagestash.stash-nio :as nio]
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
    (let [index (atom {})
          amount (.readInt input)]
      (dotimes [i amount]
        (let [[k v] (read-index-item input)]
          (swap! index assoc k v)))
      @index)))

(defn reconstruct-index [source]
  (with-open [channel (FileChannels/read source)]
    (loop [position 0
           images {}]
      (let [image (nio/read-image-from-channel-without-size channel position)
            new-position (+ position (:storage-size image))
            index-key (IndexKey. (:key image) (:size image) (:format image))
            value (IndexValue. position (:storage-size image))
            new-images (assoc images index-key value)]
        (if (< new-position (.size channel))
          (recur new-position new-images)
          new-images)))))
