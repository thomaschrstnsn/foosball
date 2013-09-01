(ns foosball.util
  (:use [hiccup.element :only [link-to]])
  (:require [noir.io :as io]
            [clojure.edn :as edn]))

(defmacro symbols-as-map
  "Inverse of descructuring as {:keys [a b c]} -
  This macro maps the symbols passed to it as keys (keyworded) with the symbols' values as value."
  [& symbols]
  (if (empty? symbols)
    {}
    (apply assoc {} (interleave (map (fn [s] (keyword (name s))) symbols)
                                (map (fn [s] s) symbols)))))

(defn link-to-player-log [{:keys [id name active]}]
  [:span
   (link-to (str "/player/log?playerid=" id) name)
   (when-not active [:span.text-error " (inactive)"])])

(defn get-player-by-name [players name]
  (->> players
       (filter (fn [p] (= name (:name p))))
       first))

(defn render-team [players team]
  (->> team
       (map #(->> %
                  (get-player-by-name players)
                  link-to-player-log))
       (interpose ", ")))

(def ^:private date-format "yyyy-MM-dd")

(defn format-datetime
  "formats the datetime using SimpleDateFormat using the format passed in as the second argument"
  [datetime fmt]
  (.format (new java.text.SimpleDateFormat fmt) datetime))

(defn format-date "Formats a date as edn compatible date"
  [d]
  (format-datetime d date-format))

(defn parsable-date?
  "Returns true when s is a string which is parsable as an edn #inst formatted date, false otherwise."
  [d]
  (try (do (edn/read-string (str "#inst \"" d "T00:00:00\""))
             true)
         (catch Exception _ false)))

(defn parse-date "Parse an edn compatible date, returns failure-value (default nil) in the case of no parse"
  ([s] (parse-date s nil))
  ([s failure-value]
     (if (parsable-date? s)
       (.parse (new java.text.SimpleDateFormat date-format) s)
       failure-value)))

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
     {:class (if (checker d) "text-success" "text-danger")})
   (printer d)])

(defn format-score [s]
  (format-value s :checker (partial < 9) :class? nil))

(def ^:private default-cljs-ns "foosball.browser.")

(defn auto-refresh-page []
  [:script (str default-cljs-ns "page_autorefresh(90)")])

;;;; compare helpers

(defn less-than? [x y]
  (neg? (compare x y)))

(defn equal-to? [x y]
  (= 0 (compare x y)))

(defn greater-than? [x y]
  (pos? (compare x y)))

(defn less-than-or-equal? [x y]
  (or (less-than? x y) (equal-to? x y)))

(defn greater-than-or-equal? [x y]
  (or (greater-than? x y) (equal-to? x y)))
