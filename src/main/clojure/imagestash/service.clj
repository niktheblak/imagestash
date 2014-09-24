(ns imagestash.service
  (:import [java.net URI]
           [java.io ByteArrayInputStream])
  (:require [imagestash.broker :as br]
            [imagestash.format :as fmt]
            [ring.adapter.jetty :as jet]
            [ring.util.response :refer :all]
            [ring.middleware.params :as params]))

(def broker (br/create-broker 1))

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
            image-source (br/from-internet-source source size :format format)
            image (br/get-or-add-image broker image-source)]
        {:status  200
         :headers {"Content-Type" (fmt/format-mime-type (:format image))}
         :body    (:stream image)})
      {:status  400
       :body    "Invalid parameters"})))

(defn -main [& args]
  (let [server (jet/run-jetty (params/wrap-params (accept-resize-route handler)) {:port 8080 :join? false})]
    (println "Started imagestash server. Press <ENTER> to quit...")
    (read-line)
    (.stop server)
    (shutdown-agents)))