(ns foosball.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(def ^:private time-format "yyyy-MM-dd")

(defn format-datetime
  "formats the datetime using SimpleDateFormat, the default format is
   \"dd-MM-yyyy\" and a custom one can be passed in as the second argument"
  ([datetime]     (format-datetime datetime time-format))
  ([datetime fmt] (.format (new java.text.SimpleDateFormat fmt) datetime)))

(defn parse-time
  ([s] (parse-time s time-format))
  ([s fmt]
     (.parse (new java.text.SimpleDateFormat fmt) s)))

(defn parse-id [s]
  (try
    (. Long parseLong s)
    (catch Exception e nil)))

(defn format-percentage [p]
  (format "%.1f%%" (double p)))

(defn format-rating [r]
  (format "%.1f" (double r)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))
