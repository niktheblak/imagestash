(ns imagestash.stash-io-test
  (:import [java.io RandomAccessFile]
           [java.util Arrays])
  (:require [clojure.test :refer :all]
            [imagestash.test-utils :refer :all]
            [imagestash.stash-io :as sio]
            [imagestash.stash-nio :as nio]))

(def test-image {:key "test-key"
                 :size 1024
                 :format :jpeg
                 :data (random-data 125)})

(deftest read-test
  (let [file (temp-file)]
    (testing "write/read roundtrip"
      (let [stored-image (nio/write-image-to-file file test-image)
            ra-file (RandomAccessFile. file "r")
            read-image (sio/read-image-from-file ra-file)]
        (is (= "test-key" (:key read-image)))
        (is (= 1024 (:size read-image)))
        (is (= :jpeg (:format read-image)))
        (is (Arrays/equals (:data test-image) (:data read-image)))
        (is (= (:size stored-image) (:stored-length read-image)))))
    (testing "read should throw on invalid header"
      (let [ra-file (RandomAccessFile. file "r")
            _ (.seek ra-file 3)]
        (is (thrown-with-msg? RuntimeException #"Invalid header" (sio/read-image-from-file ra-file)))))))
