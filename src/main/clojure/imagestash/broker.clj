(ns imagestash.broker
  (:import [java.io File RandomAccessFile]
           [java.security MessageDigest]
           [imagestash.j Base58]
           [java.net URL URI MalformedURLException])
  (:require [clojure.java.io :as jio]
            [imagestash.io :as io]
            [imagestash.digest :as d]
            [imagestash.stash :as stash]
            [imagestash.index :as index]
            [imagestash.resize :as resize]
            [imagestash.format :as format]))

(defn get-key [source size format]
  (let [digest (d/digest source size format)]
    (Base58/encode digest)))

(defn- to-url [source]
  (cond
    (instance? URL source) source
    (instance? URI source) (.toURL source)
    (string? source) (URL. source)
    :else (throw (ex-info "Unsupported source" {:source source}))))

(defn from-internet-source [source size & {:keys [format] :or {format :jpeg}}]
  {:pre [(number? size)
         (format/supported-format? format)]
   :post [(:key %)
          (:size %)
          (:format %)
          (:source %)]}
  {:key    (get-key source size format)
   :size   size
   :format format
   :source (to-url source)})

(defn from-file-source [^File source size & {:keys [format] :or {format :jpeg}}]
  {:pre [(.exists source)
         (number? size)
         (format/supported-format? format)]
   :post [(:key %)
          (:size %)
          (:format %)
          (:source %)]}
  {:key    (get-key (.getAbsolutePath source) size format)
   :size   size
   :format format
   :source source})

(defn storage-file [id]
  (File. (str "broker-" id ".bin")))

(defn create-broker [id]
  (agent {:broker-id id
          :storage (storage-file id)
          :index {}}))

(defn- add-image-fn [broker {:keys [key size format source] :as image}]
  (let [storage (:storage broker)
        index-key (index/->IndexKey key size format)
        resized-image-data (resize/resize-image source size format)
        resized-image (assoc image :data resized-image-data)
        {:keys [size-on-disk offset]} (stash/write-to storage resized-image)
        value (index/->IndexValue offset size-on-disk)]
    (assoc-in broker [:index index-key] value)))

(defn add-image [broker {:keys [key size format source] :as image}]
  {:pre [(string? key)
         (pos? size)
         (format/supported-format? format)
         (resize/supported-source? source)]}
  (send-off broker add-image-fn image))

(defn get-image [broker {:keys [key size format]}]
  {:pre [(string? key)
         (pos? size)
         (format/supported-format? format)]}
  (let [index (:index @broker)
        storage (:storage @broker)
        index-key (index/->IndexKey key size format)
        image (get index index-key)]
    (if image
      (stash/read-from storage (:offset image))
      nil)))

(defn get-or-add-image [broker image]
  {:pre [(:source image)]}
  (let [cached-image (get-image broker image)]
    (if cached-image
      cached-image
      (do
        (add-image broker image)
        (await-for 30000 broker)
        (get-image broker image)))))