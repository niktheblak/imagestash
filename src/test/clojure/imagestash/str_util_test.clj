(ns imagestash.str-util-test
  (:require [clojure.test :refer :all]
            [imagestash.str-util :refer :all]))

(deftest str-util-test
  (testing "pad"
    (is (= "00123" (pad "123" 5 \0)))
    (is (= "123" (pad "123" 3 \0)))
    (is (= "123" (pad "123" 2 \0)))
    (is (= "00123" (pad "123" 5 0)))
    (is (= "00123" (pad "123" 5 "0")))
    (is (= "000" (pad "" 3 "0")))
    (is (= "strst" (pad "" 5 "str")))
    (is (= "st123" (pad "123" 5 "str"))))

  (testing "hexify"
    (is (= "01" (hexify 1)))
    (is (= "ab" (hexify 0xAB)))
    (is (= "cafebabe" (hexify 0xCAFEBABE))))

  (testing "format-bytes"
    (is (= "cafebabe" (format-bytes 0xCAFEBABE)))
    (is (= "cafebabe" (format-bytes [0xCA 0xFE 0xBA 0xBE])))
    (is (= "01ff02" (format-bytes [0x1 0xFF 0x2])))))
