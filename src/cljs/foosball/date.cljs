(ns foosball.date
  (:require [cljs-time.core :as dt]
            [cljs-time.coerce :as dc]
            [cljs-time.format :as df]
            [clojure.string :as str]
            [goog.date :as date]))

(def formatter (df/formatter "dd/MM/yyyy") )

(defn str->date [s]
  (df/parse formatter s))

(defn ->str [d]
  (when d
    (df/unparse formatter (dc/to-date-time d))))

(defn iso8601-from-date [d]
  (->> d
       dc/from-date
       (df/unparse (df/formatters :date-time))))

(extend-protocol IPrintWithWriter
  js/Date
  (-pr-writer [o writer _]
    (-write writer (str "#inst \"" "jefferson" "\"")))

  goog.date.DateTime
  (-pr-writer [o writer _]
    (let [iso8601 (iso8601-from-date o)]
      (-write writer (str "#inst \"" iso8601 "\"")))))
