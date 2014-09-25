(ns imagestash.service
  (:import [java.net URI]
           [java.io ByteArrayInputStream File])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [imagestash.index :as idx]
            [ring.adapter.jetty :as jet]
            [ring.util.response :refer :all]
            [ring.middleware.params :as params]))

(def index-file-name "index.bin")

(defn- load-index []
  (let [file (File. index-file-name)]
    (if (.exists file)
        (let [index (idx/load-index file)]
          (println "Loaded" (count index) "images from index file" index-file-name)
          index)
      {})))

(defn accept-resize-route [handler]
  (fn [request]
    (let [path (:uri request)]
      (if (= "/resize" path)
        (if (and
              (get-in request [:params "source"])
              (get-in request [:params "size"]))
          (handler request)
          {:status 400
           :headers {"Content-Type" "text/plain"}
           :body   "Invalid parameters"})
        (not-found (str "Path" path " not found"))))))

(defn handler [broker request]
  (let [source (get-in request [:params "source"])
        raw-size (get-in request [:params "size"])
        raw-format (get-in request [:params "format"] "jpeg")]
      (let [size (Integer/parseInt raw-size)
            format (fmt/parse-format raw-format)
            image-source (br/from-internet-source source size :format format)]
        (if-let [image (br/get-or-add-image broker image-source)]
          {:status  200
           :headers {"Content-Type" (fmt/format-mime-type (:format image))}
           :body    (:stream image)}
          (not-found (str "Image with source " source " was not found"))))))

(defn -main []
  (let [broker (br/create-broker 1 :index (load-index))
        handler (accept-resize-route (partial handler broker))
        server (jet/run-jetty (params/wrap-params handler) {:port 8080 :join? false})]
    (println "Started imagestash server. Press <ENTER> to quit...")
    (read-line)
    (println "Stopping imagestash server")
    (.stop server)
    (println "Saving index with" (count (:index @broker)) "images to file" index-file-name)
    (idx/save-index index-file-name (:index @broker))
    (shutdown-agents)
    (println "System shutdown completed")))