(ns imagestash.flags-test
  (:require [clojure.test :refer :all]
            [imagestash.flags :refer :all]))

(def test-flag-values [:a :b :c :d :e :f :g :h])

(def test-encode (partial encode-set test-flag-values))

(def test-decode (partial decode-set test-flag-values))

(deftest set-read-write-test
  (testing "encode all flags set correctly"
    (is (= 2r11111111 (test-encode test-flag-values))))
  (testing "encode test set correctly"
    (is (= 2r100101 (test-encode #{:a :c :f}))))
  (testing "encode decode roundtrip"
    (is (= #{:a :c :f} (test-decode (test-encode #{:a :c :f})))))
  (testing "throw on unknown flags"
    (is (thrown? IllegalArgumentException (encode-set test-flag-values #{:a :q}))))
  (testing "throw on too many set bits"
    (is (thrown? IllegalArgumentException (decode-set [:a :b] 2r1111)))))

(deftest flags-read-write-test
  (testing "encode flags correctly"
    (is (= 1 (encode-flags #{:deleted}))))
  (testing "decode flags correctly")
    (is (= #{:deleted} (decode-flags 1))))
