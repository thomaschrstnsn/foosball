(ns foosball.util
  (:use [hiccup.element :only [link-to]])
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn link-to-player-log [{:keys [id name]}]
  (link-to (str "/player/log?playerid=" id) name))

(defn get-player-by-name [players name]
  (->> players (filter (fn [p] (= name (:name p)))) first))

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

(defn format-percentage
  ([p] (format-percentage 1 p))
  ([digits p] (format (str "%." digits "f%%") (double p))))

(defn format-permil [p]
  (format "%.2f‰" (double p)))

(defn format-rating [r]
  (format "%.1f" (double r)))

(defn format-value
  "Formats a value with text-success and text-error classes, based on optional checker.
   Defaults to pos? such that positive numbers are text-success and negative are text-error.
   Values failing the optional class? predicate are not given a class.
   Default 0 is not given a class. With class? nil everthing is given a class
   Optional argument printer is used to format value to string, defaults to str"
  [d & {:keys [checker class? printer container-tag] :or {checker pos?
                                                          class?  (partial not= 0)
                                                          printer str
                                                          container-tag :div}}]
  [container-tag
   (when (or (nil? class?) (class? d))
     {:class (if (checker d) "text-success" "text-error")})
   (printer d)])

(defn format-score [s]
  (format-value s :checker (partial < 9) :class? nil))

(defn auto-refresh-page []
  [:script "foosball.main.page_autorefresh(90)"])
