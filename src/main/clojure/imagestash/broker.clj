(ns imagestash.broker
  (:import [java.io File]
           [imagestash.j Base58]
           [java.net URL URI])
  (:require [imagestash.digest :as d]
            [imagestash.stash-nio :as snio]
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
  {:key    (get-key (str source) size (str format))
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

(defn create-broker [id & {:keys [index] :or {index {}}}]
  (agent {:broker-id id
          :storage (storage-file id)
          :index index}))

(defn- add-image-fn [broker {:keys [key size format source] :as image}]
  (let [storage (:storage broker)
        index-key (index/->IndexKey key size format)
        resized-image-data (resize/resize-image source size format)
        resized-image (assoc image :data resized-image-data)
        stored-image (snio/write-image-to-file storage resized-image)
        value (index/->IndexValue (:offset stored-image) (:size stored-image))]
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
        index-key (index/->IndexKey key size format)]
    (if-let [image-pointer (get index index-key)]
      (snio/read-image-from-file storage (:offset image-pointer) (:size image-pointer))
      nil)))

(defn get-or-add-image [broker image]
  {:pre [(:source image)]}
  (if-let [cached-image (get-image broker image)]
    cached-image
    (do
      (add-image broker image)
      ; TODO: Figure out a way to do this more efficiently/robustly
      (await-for 30000 broker)
      (get-image broker image))))