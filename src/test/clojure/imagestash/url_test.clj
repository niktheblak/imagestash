(ns imagestash.url-test
  (:import [java.net URL URI])
  (:require [clojure.test :refer :all]
            [imagestash.url :refer :all]))

(def test-url "http://www.url.com")

(deftest instance-check-test
  (testing "should detect Java URL"
    (is (url? (URL. test-url))))
  (testing "should detect Java URI"
    (is (url? (URI. test-url))))
  (testing "should detect URL string"
    (is (url? test-url)))
  (testing "should not detect an empty string"
    (is (not (url? ""))))
  (testing "should not detect non-URL string"
    (is (not (url? "not an URL")))))

(deftest url-conversion-test
  (testing "should pass Java URL as is"
    (let [java-url (URL. test-url)]
      (is (identical? java-url (to-url java-url)))))
  (testing "should convert Java URI to URL"
    (is (instance? URL (to-url (URI. test-url)))))
  (testing "should convert plain string to URL"
    (is (instance? URL (to-url test-url))))
  (testing "should throw on unknown input"
    (is (thrown-with-msg? RuntimeException #"Unsupported source" (to-url 6)))))
