(ns foosball.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn format-time
  "formats the time using SimpleDateFormat, the default format is
   \"dd-MM-yyyy\" and a custom one can be passed in as the second argument"
  ([time] (format-time time "dd-MM-yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn parse-time
  ([s] (parse-time s "dd-MM-yyyy"))
  ([s fmt]
     (.parse (new java.text.SimpleDateFormat fmt) s)))

(defn parse-int [s]
  (try
    (. Integer parseInt s)
    (catch Exception e nil)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))
