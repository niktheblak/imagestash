(ns imagestash.stash-nio-test
  (:import [java.io File]
           [java.util Arrays Random])
  (:require [clojure.test :refer :all]
            [imagestash.test-utils :refer :all]
            [imagestash.stash-core :as stash]
            [imagestash.stash-nio :as nio]))

(defn- divisible-by-eight [n]
  (= 0 (mod n 8)))

(def test-image {:key "test-key"
                 :size 1024
                 :format :jpeg
                 :data (random-data 125)})

(deftest write-test
  (let [file (temp-file)]
    (testing "write to an empty file"
      (let [{:keys [size]} (nio/write-image-to-file file test-image)
            file-len (.length file)]
        (is (divisible-by-eight size))
        (is (= 168 size))
        (is (= size file-len))))
    (testing "append to a non-empty file"
      (let [stored-image (nio/write-image-to-file file (assoc test-image :key "test-key-2"))
            file-len (.length file)]
        (is (= 168 (:size stored-image)))
        (is (= 336 file-len))))))

(deftest read-test
  (let [file (temp-file)
        stored-image (nio/write-image-to-file file test-image)
        file-size (.length file)]
    (testing "write/read roundtrip"
      (let [read-image (nio/read-image-from-file file 0 file-size)]
        (is (= "test-key" (:key read-image)))
        (is (= 1024 (:size read-image)))
        (is (= :jpeg (:format read-image)))
        (is (Arrays/equals (:data test-image) (:data read-image)))
        (is (= (:size stored-image) (:stored-length read-image)))))
    (testing "read should throw on invalid header"
      (is (thrown-with-msg? RuntimeException #"Invalid header" (nio/read-image-from-file file 3 (- file-size 3)))))))

(deftest size-on-disk-test
  (let [file (temp-file)]
    (testing "size-on-disk"
      (let [expected-size (stash/size-on-disk test-image)
            {:keys [size]} (nio/write-image-to-file file test-image)
            file-size (.length file)
            read-image (nio/read-image-from-file file 0 file-size)]
        (is (= expected-size size))
        (is (= expected-size file-size))
        (is (= expected-size (:stored-length read-image)))))))