; Copyright 2016 Niko Korhonen
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0

; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns imagestash.index
  (:import [java.io DataOutputStream DataInputStream]
           [clojure.lang Keyword]
           [imagestash.j FileChannels])
  (:require [imagestash.image-core :as core]
            [imagestash.image-io :as iio]
            [clojure.java.io :as jio]))

(defrecord IndexKey [^String key ^Long size ^Keyword format])

(defrecord IndexValue [^Long offset ^Long size])

(defn- read-index-item [^DataInputStream input]
  (let [key (.readUTF input)
        image-size (.readInt input)
        format-code (.readByte input)
        format (core/code-to-format format-code)
        offset (.readLong input)
        size (.readLong input)
        k (IndexKey. key image-size format)
        v (IndexValue. offset size)]
    [k v]))

(defn- write-index-item [^DataOutputStream output k v]
  (doto output
    (.writeUTF (:key k))
    (.writeInt (int (:size k)))
    (.write (core/format-to-code (:format k)))
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
