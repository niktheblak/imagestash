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
      (idx/load-index file)
      {})))

(def broker (br/create-broker 1 :index (load-index)))

(defn accept-resize-route [handler]
  (fn [request]
    (let [path (:uri request)]
      (if (= "/resize" path)
        (handler request)
        (not-found (str "Path" path " not found"))))))

(defn handler [request]
  (let [source (get-in request [:params "source"])
        raw-size (get-in request [:params "size"])
        raw-format (get-in request [:params "format"] "jpeg")]
    (if (and source raw-size)
      (let [size (Integer/parseInt raw-size)
            format (fmt/parse-format raw-format)
            image-source (br/from-internet-source source size :format format)]
        (if-let [image (br/get-or-add-image broker image-source)]
          {:status  200
           :headers {"Content-Type" (fmt/format-mime-type (:format image))}
           :body    (:stream image)}
          (not-found (str "Image with source " source " was not found"))))
      {:status  400
       :body    "Invalid parameters"})))

(defn -main []
  (let [server (jet/run-jetty (params/wrap-params (accept-resize-route handler)) {:port 8080 :join? false})]
    (println "Started imagestash server. Press <ENTER> to quit...")
    (read-line)
    (.stop server)
    (idx/save-index index-file-name (:index @broker))
    (shutdown-agents)))