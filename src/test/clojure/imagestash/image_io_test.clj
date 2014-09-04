(ns imagestash.image-io-test
  (:import [java.io File]
           [java.util Arrays Random])
  (:require [clojure.test :refer :all]
            [imagestash.stash :as image]))

(defn- temp-file []
  (let [file (File/createTempFile "file-test-" ".tmp")
        _ (.deleteOnExit file)]
    file))

(defn- divisible-by-eight [n]
  (= 0 (mod n 8)))

(def test-image-data
  (let [data (byte-array 125)
        random (Random.)
        _ (.nextBytes random data)]
    data))

(def test-image {:key "test-key"
                 :size 1024
                 :format "JPEG"
                 :data test-image-data})

(deftest write-test
  (let [file (temp-file)]
    (testing "write to an empty file"
      (let [{:keys [size-on-disk]} (image/write-to file test-image)
            file-len (.length file)]
        (is (divisible-by-eight size-on-disk))
        (is (= 152 size-on-disk))
        (is (= size-on-disk file-len))))
    (testing "append to a non-empty file"
      (let [{:keys [size-on-disk]} (image/write-to file (assoc test-image :key "test-key-2"))
            file-len (.length file)]
        (is (= 160 size-on-disk))
        (is (= 312 file-len))))))

(deftest read-test
  (let [file (temp-file)]
    (testing "write/read roundtrip"
      (let [{:keys [size-on-disk]} (image/write-to file test-image)
            read-image (image/read-from file 0)]
        (is (= "test-key" (:key read-image)))
        (is (= 1024 (:size read-image)))
        (is (= "JPEG" (:format read-image)))
        (is (Arrays/equals test-image-data (:data read-image)))
        (is (= size-on-disk (:stored-length read-image)))))
    (testing "read should throw on invalid header"
      (is (thrown-with-msg? RuntimeException #"Invalid header" (image/read-from file 3))))))

(deftest size-on-disk-test
  (let [file (temp-file)]
    (testing "size-on-disk"
      (let [expected-size (image/size-on-disk test-image)
            {:keys [size-on-disk]} (image/write-to file test-image)
            file-size (.length file)
            read-image (image/read-from file 0)]
        (is (= expected-size size-on-disk))
        (is (= expected-size file-size))
        (is (= expected-size (:stored-length read-image)))))))