(ns imagestash.broker
  (:import [java.io File]
           [imagestash.j Base58])
  (:require [imagestash.digest :as d]
            [imagestash.stash-nio :as snio]
            [imagestash.index :as index]
            [imagestash.resize :as resize]
            [imagestash.format :as format]
            [imagestash.url :as url]))

(defn get-key [source size format]
  (let [digest (d/digest source size format)]
    (Base58/encode digest)))

(defn from-internet-source [source size & {:keys [format] :or {format :jpeg}}]
  {:pre [source
         (number? size)
         (format/supported-format? format)]
   :post [(:key %)
          (:size %)
          (:format %)
          (:source %)]}
  {:key    (get-key (str source) size (str format))
   :size   size
   :format format
   :source (url/to-url source)})

(defn from-local-source [key size & {:keys [format] :or {format :jpeg}}]
  {:pre [(string? key)
         (number? size)
         (format/supported-format? format)]
   :post [(:key %)
          (:size %)
          (:format %)]}
  {:key    key
   :size   size
   :format format})

(defn storage-file [id]
  (File. (str "broker-" id ".bin")))

(defn create-broker [id & {:keys [index] :or {index {}}}]
  {:broker-id id
   :storage (storage-file id)
   :index index})

(defn contains-image? [broker {:keys [key size format]}]
  (let [index-key (index/->IndexKey key size format)]
    (get (:index broker) index-key)))

(defn get-image [{:keys [index storage]} {:keys [key size format]}]
  (let [index-key (index/->IndexKey key size format)]
    (if-let [image-pointer (get index index-key)]
      (snio/read-image-from-file storage (:offset image-pointer) (:size image-pointer))
      nil)))

(defn add-image [broker {:keys [key size format source] :as image}]
  {:pre [source
         (string? key)
         (pos? size)
         (format/supported-format? format)]}
  (if (contains-image? broker image)
    broker
    (let [storage (:storage broker)
          index-key (index/->IndexKey key size format)
          resized-image-data (resize/resize-image source size format)
          resized-image (assoc image :data resized-image-data)
          stored-image (snio/write-image-to-file storage resized-image)
          index-value (index/->IndexValue (:offset stored-image) (:stored-length stored-image))]
      (assoc-in broker [:index index-key] index-value))))
