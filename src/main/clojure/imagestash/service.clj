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

(ns imagestash.service
  (:import [java.io ByteArrayInputStream File])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [imagestash.index :as idx]
            [imagestash.io :as io]
            [imagestash.url :as url]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.api.middleware :as mw]
            [compojure.api.swagger :as swag]
            [schema.core :as s]
            [clojure.tools.logging :as log]))

(def index-file (File. "index.bin"))

(def broker-storage-file (br/storage-file 1))

(def broker (atom nil))

(defn load-index []
  (cond
    (io/file-exists? index-file)
    (let [index (idx/load-index index-file)]
      (log/info "Loaded" (count index) "images from index file" (.getAbsolutePath index-file))
      index)
    (io/file-exists? broker-storage-file)
    (let [index (idx/reconstruct-index broker-storage-file)]
      (log/info "Reconstructed index with" (count index) "images from storage file" (.getAbsolutePath broker-storage-file))
      index)
    :else
    (do
      (log/info "Starting with empty index")
      {})))

(defn start []
  (let [index (load-index)
        br (br/create-broker 1 :index index)]
    (log/info "Created image broker" (:broker-id br) "using storage file" (.getAbsolutePath (:storage br)))
    (reset! broker br))
  (log/info "Started imagestash server"))

(defn stop []
  (log/info "Saving index with" (count (:index @broker)) "images to file" (.getAbsolutePath index-file))
  (idx/save-index index-file (:index @broker))
  (shutdown-agents)
  (log/info "System shutdown completed"))

(defn- get-location [image]
  (str
    "/resize?key="
    (:key image)
    "&size="
    (:size image)
    "&format="
    (fmt/format-name (:format image))))

(defn render-image [image]
  (let [length (alength (:data image))
        stream (ByteArrayInputStream. (:data image))]
    {:status  200
     :headers {"Content-Type"   (fmt/format-mime-type (:format image))
               "Content-Length" (str length)
               "Location"       (get-location image)}
     :body    stream}))

(defn- fetch-from-remote-source [{:keys [source size format]}]
  {:pre [(url/url? source)]}
  (let [image-source (br/from-internet-source source size :format format)
        br (br/add-image @broker image-source)]
    (reset! broker br)
    (br/get-image br image-source)))

(defn- fetch-from-local-source [{:keys [key size format]}]
  {:pre [(string? key)]}
  (let [image-source (br/from-local-source key size :format format)]
    (br/get-image @broker image-source)))

(defapi app
  (middleware [mw/api-middleware])
  (swag/swagger-ui)
  (swag/swagger-docs)
  (GET "/resize" []
    :query-params [{source :- s/Str nil}, {key :- s/Str nil}, size :- s/Int, {format :- s/Str "jpeg"}]
    :summary "return image from specified source resized to desired size"
    (let [parsed-format (fmt/parse-format format)]
      (when-not (or key source)
        (bad-request! "Parameter key or source must be specified"))
      (if key
        (if-let [image (fetch-from-local-source
                         {:key key
                          :size size
                          :format parsed-format})]
          (render-image image)
          (not-found (str "Image with key " key " not found")))
        (let [image (fetch-from-remote-source
                      {:source source
                       :size size
                       :format parsed-format})]
          (render-image image))))))
