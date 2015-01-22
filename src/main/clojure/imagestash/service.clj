(ns imagestash.service
  (:import [java.io ByteArrayInputStream File])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [imagestash.index :as idx]
            [imagestash.io :as io]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.middleware.defaults :as rmd]
            [clojure.tools.logging :as log]))

(def index-file (File. "index.bin"))

(def broker-storage-file (br/storage-file 1))

(def broker (ref nil))

(defn load-index []
  (cond
    (io/file-exists? index-file)
      (let [index (idx/load-index index-file)]
        (log/info "Loaded" (count index) "images from index file" index-file)
        index)
    (io/file-exists? broker-storage-file)
      (let [index (idx/reconstruct-index broker-storage-file)]
        (log/info "Reconstructed index with" (count index) "images from storage file" broker-storage-file)
        index)
    :else
      (do
        (log/info "Starting with empty index")
        {})))

(defn start []
  (let [index (load-index)
        br (br/create-broker 1 :index index)]
    (dosync
      (ref-set broker br)))
  (log/info "Started imagestash server"))

(defn stop []
  (log/info "Saving index with" (count (:index @broker)) "images to file" index-file)
  (idx/save-index index-file (:index @broker))
  (shutdown-agents)
  (log/info "System shutdown completed"))

(deftype RenderableImage [image]
  response/Renderable
  (render [this request]
    (let [length (alength (:data image))
          stream (ByteArrayInputStream. (:data image))]
      {:status  200
       :headers {"Content-Type"   (fmt/format-mime-type (:format image))
                 "Content-Length" (str length)}
       :body    stream})))

(defn resize-handler [source raw-size raw-format]
  (let [size (Integer/parseInt raw-size)
        format (if raw-format
                 (fmt/parse-format raw-format)
                 :jpeg)
        image-source (br/from-internet-source source size :format format)]
    (let [br (br/add-image @broker image-source)
          image (br/get-image br image-source)]
      (dosync
        (ref-set broker br))
      (RenderableImage. image))))

(defroutes resize-routes
  (GET "/resize" [source size format] (resize-handler source size format)))

(def app
  (handler/site (rmd/wrap-defaults resize-routes rmd/api-defaults)))
