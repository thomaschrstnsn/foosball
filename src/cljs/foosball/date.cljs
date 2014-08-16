(ns foosball.date
  (:require [cljs-time.core :as dt]
            [cljs-time.coerce :as dc]
            [cljs-time.format :as df]
            [clojure.string :as str]))

(def formatter (df/formatter "dd/MM/yyyy") )

(defn str->date [s]
  (df/parse formatter s))

(defn ->str [d]
  (when d
    (df/unparse formatter (dc/to-date-time d))))
