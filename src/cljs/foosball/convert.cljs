(ns foosball.convert)

(defn str->int [s]
  (when (re-matches #"^-?\d+$" s)
    (js/parseInt s)))

(defn ->int [x]
  (cond
   (string? x)  (str->int x)
   (integer? x) x))
