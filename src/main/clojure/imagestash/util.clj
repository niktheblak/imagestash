(ns imagestash.util)

(defn printable? [c]
  (Character/isLetterOrDigit (int c)))

(defn to-printable [c]
  (if (printable? c)
    c
    \.))

(defn printables [arr]
  (apply str (map to-printable arr)))

(defn to-hex [c]
  (let [hex (Integer/toHexString (int c))]
    (if (= 1 (.length hex))
      (str "0" hex)
      hex)))

(defn hex-string [arr]
  (let [hexes (map #(str (to-hex %1) " ") arr)]
    (apply str hexes)))
