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

(defn storage-file [id]
  (File. (str "broker-" id ".bin")))

(defn create-broker [id & {:keys [index] :or {index {}}}]
  {:broker-id id
   :storage (storage-file id)
   :index index})

(defn- get-image [{:keys [index storage]} {:keys [key size format]}]
  (let [index-key (index/->IndexKey key size format)]
    (if-let [image-pointer (get index index-key)]
      (snio/read-image-from-file storage (:offset image-pointer) (:size image-pointer))
      nil)))

(defn get-or-add-image [broker {:keys [key size format source] :as image}]
  {:pre [(:source image)
         (string? key)
         (pos? size)
         (format/supported-format? format)]
   :post [(:data %)]}
  (if-let [cached-image (get-image broker image)]
    cached-image
    (let [storage (:storage broker)
          index-key (index/->IndexKey key size format)
          resized-image-data (resize/resize-image source size format)
          resized-image (assoc image :data resized-image-data)
          stored-image (snio/write-image-to-file storage resized-image)
          index-value (index/->IndexValue (:offset stored-image) (:size stored-image))]
      (assoc resized-image
             :added true
             :index-key index-key
             :index-value index-value))))