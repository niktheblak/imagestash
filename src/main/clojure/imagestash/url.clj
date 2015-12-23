(ns imagestash.url
  (:import [java.net URL URI MalformedURLException]))

(defn java-url? [source]
  (instance? URL source))

(defn java-uri? [source]
  (instance? URI source))

(defn java-url-like? [source]
  (or
    (java-url? source)
    (java-uri? source)))

(defn parses-to-url? [source]
  (try
    (URL. source)
    true
    (catch MalformedURLException e false)))

(defn to-url [source]
  (cond
    (java-url? source) source
    (java-uri? source) (.toURL source)
    (and
      (string? source)
      (parses-to-url? source)) (URL. source)
    :else nil))

(defn url? [source]
  (cond
    (java-url-like? source) true
    (and
      (string? source)
      (not (.isEmpty source))) (parses-to-url? source)
    :else false))
