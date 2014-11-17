(ns imagestash.resize-test
  (:import [java.io File ByteArrayInputStream]
           [javax.imageio ImageIO])
  (:require [clojure.test :refer :all]
            [imagestash.resize :as resize]))

(def source-image (File. "src/test/resources/ABC_FlockFav_2014_1024x500_1.jpg"))

(deftest resize-test
  (testing "resize"
    (let [resized-data (resize/resize-image source-image 600 :jpeg)]
      (resize/with-image [resized-image (ImageIO/read (ByteArrayInputStream. resized-data))]
        (is (= 600 (.getWidth resized-image)))
        (is (= 293 (.getHeight resized-image)))))))
