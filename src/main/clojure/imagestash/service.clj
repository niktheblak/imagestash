(ns imagestash.service
  (:import [java.io ByteArrayInputStream File])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [imagestash.index :as idx]
            [imagestash.io :as io]
            [imagestash.url :as url]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [ring.util.response :as resp]
            [compojure.response :as cresp]
            [ring.middleware.defaults :as rmd]
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

(deftype RenderableImage [image]
  cresp/Renderable
  (render [this request]
    (let [length (alength (:data image))
          stream (ByteArrayInputStream. (:data image))]
      {:status  200
       :headers {"Content-Type"   (fmt/format-mime-type (:format image))
                 "Content-Length" (str length)
                 "Location"       (get-location image)}
       :body    stream})))

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

(defn resize-handler [request]
  (if-let [image (if (:key request)
                   (fetch-from-local-source request)
                   (fetch-from-remote-source request))]
    (RenderableImage. image)
    (resp/not-found (str "Image with key " key " not found"))))

(defn require-param [condition param-name]
  (when-not condition
    (throw (IllegalArgumentException. (str "Parameter " param-name " must be specified")))))

(defn- parse-args [key source size format]
  (try
    (require-param (or key source) "key or source")
    (require-param size "size")
    {:key key
     :source source
     :size (Integer/parseInt size)
     :format (if format
               (fmt/parse-format format)
               :jpeg)}
    (catch IllegalArgumentException e {:error (.getMessage e)})
    (catch NumberFormatException e {:error (.getMessage e)})))

(defn bad-request [& message]
  {:status  400
   :headers {"Content-Type" "text/plain"}
   :body    message})

(defroutes resize-routes
           (GET "/resize" [key source size format]
                (let [args (parse-args key source size format)
                      error (:error args)]
                  (if error
                    (bad-request error)
                    (resize-handler args)))))

(def app
  (handler/site (rmd/wrap-defaults resize-routes rmd/api-defaults)))
