(ns imagestash.service-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [imagestash.service :as service]))

(deftest service-test
  (testing "/resize without key or source"
    (let [response (service/app (mock/request :get "/resize" {:size 200}))]
      (is (= (:status response) 400))
      (is (= (first (:body response)) "Parameter key or source must be specified"))))
  (testing "/resize without size"
    (let [response (service/app (mock/request :get "/resize" {:key "testKey"}))]
      (is (= (:status response) 400))
      (is (= (first (:body response)) "Parameter size must be specified")))))
