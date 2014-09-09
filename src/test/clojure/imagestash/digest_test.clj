(ns imagestash.digest-test
  (:require [clojure.test :refer :all]
            [imagestash.digest :refer :all]
            [imagestash.io :as io]))

(deftest digest-test
  (testing "digest with byte arrays"
    (let [d1 (digest (io/to-bytes [1 2 3]))
          d2 (digest (io/to-bytes [1 2]) (io/to-bytes [3]))
          d3 (digest (io/to-bytes [1]) (io/to-bytes [2]) (io/to-bytes [3]))]
    (is (= (seq d1) (seq d2) (seq d3)))))
  (testing "digest with mixed input"
    (let [d1 (digest 1 2 (io/to-bytes [3]))
          d2 (digest [1 2 3])
          d3 (digest [1] [2] [3])]
      (is (= (seq d1) (seq d2) (seq d3)))))
  (testing "digest with text"
    (let [d1 (digest "hello")
          d2 (digest "hel" "lo")]
      (is (= (seq d1) (seq d2))))))