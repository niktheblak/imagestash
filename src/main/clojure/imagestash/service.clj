(ns imagestash.service
  (:import [java.io ByteArrayInputStream File])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [imagestash.index :as idx]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.util.response :as response]
            [ring.middleware.defaults :as rmd]))

(def index-file-name "index.bin")

(def broker (ref nil))

(defn load-index [file-name]
  (let [file (File. file-name)]
    (if (.exists file)
        (let [index (idx/load-index file)]
          (println "Loaded" (count index) "images from index file" index-file-name)
          index)
      {})))

(defn start []
  (let [index (load-index index-file-name)
        br (br/create-broker 1 :index index)]
    (dosync
      (ref-set broker br)))
  (println "Started imagestash server"))

(defn stop []
  (println "Saving index with" (count (:index @broker)) "images to file" index-file-name)
  (idx/save-index index-file-name (:index @broker))
  (shutdown-agents)
  (println "System shutdown completed"))


(defn resize-handler [source raw-size raw-format]
  (let [size (Integer/parseInt raw-size)
        format (if raw-format
                 (fmt/parse-format raw-format)
                 :jpeg)
        image-source (br/from-internet-source source size :format format)]
    (if-let [image (br/get-or-add-image @broker image-source)]
      (let [length (alength (:data image))
            stream (ByteArrayInputStream. (:data image))]
        {:status  200
         :headers {"Content-Type"   (fmt/format-mime-type (:format image))
                   "Content-Length" (str length)}
         :body    stream})
      (response/not-found (str "Image with source " source " was not found")))))

(defroutes resize-routes
  (GET "/resize" [source size format] (resize-handler source size format)))

(def app
  (handler/site (rmd/wrap-defaults resize-routes rmd/api-defaults)))
