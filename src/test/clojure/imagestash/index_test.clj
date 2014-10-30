(ns imagestash.index-test
  (:import [java.io File]
           [java.util Arrays Random])
  (:require [clojure.test :refer :all]
            [imagestash.stash-io :as stash]
            [imagestash.test-utils :refer :all]
            [imagestash.index :refer :all]))

(defn- test-image [key size] {:key key
                 :size 1024
                 :format :jpeg
                 :data (random-data size)})

(def test-index {(->IndexKey "img1" 1024 :jpeg) (->IndexValue 0 160)
                 (->IndexKey "img2" 1024 :jpeg) (->IndexValue 160 160)
                 (->IndexKey "img3" 1024 :jpeg) (->IndexValue 320 160)})

(deftest save-index-test
  (testing "save index"
    (let [file (temp-file)
          _ (save-index file test-index)]
      (is (= 85 (.length file))))))

(deftest index-roundtrip-test
  (testing "index save/load roundtrip"
    (let [file (temp-file)
          _ (save-index file test-index)
          index (load-index file)]
      (is (= test-index index)))))

(deftest reconstruct-index-test
  (testing "reconstruct index from storage file"
    (let [file (temp-file)
          img1 (test-image "img1" 125)
          img2 (test-image "img2" 125)
          img3 (test-image "img3" 125)]
      (stash/write-image-to-file file img1)
      (stash/write-image-to-file file img2)
      (stash/write-image-to-file file img3)
      (let [index (reconstruct-index file)]
        (is (= test-index index))))))