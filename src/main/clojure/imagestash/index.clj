(ns imagestash.index
  (:import [java.io File RandomAccessFile]
           [clojure.lang Keyword])
  (:require [imagestash.stash :as iio]))

(defrecord IndexKey [^String key ^Long size ^Keyword format])

(defrecord IndexValue [^Long offset ^Long size])

(defn index-key [{:keys [key size format]}]
  (str key size format))

(defn save-index [target index]
  (spit target index))

(defn load-index [target]
  (read-string (slurp target)))

(defn reconstruct-index [source]
  (with-open [ra-file (RandomAccessFile. source "r")]
    (loop [bytes-read 0
           images {}]
      (let [current-offset (.getFilePointer ra-file)
            image (iio/read-from-file ra-file current-offset)
            new-bytes-read (+ bytes-read (:stored-length image))
            index-key (IndexKey. (:key image) (:size image) (:format image))
            value (IndexValue. current-offset (:stored-length image))
            new-images (assoc images index-key value)]
        (if (< new-bytes-read (.length ra-file))
          (recur new-bytes-read new-images)
          new-images)))))