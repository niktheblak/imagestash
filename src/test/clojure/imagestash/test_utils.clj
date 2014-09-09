(ns imagestash.test-utils
  (:import [java.io File]
           [java.util Random]))

(defn temp-file []
  (let [file (File/createTempFile "imagestash-test-" ".tmp")]
    (.deleteOnExit file)
    file))

(defn random-data [size]
  (let [data (byte-array size)
        random (Random.)]
    (.nextBytes random data)
    data))
