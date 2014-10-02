(ns foosball.software
  (:require [schema.core :as s]))

(defn- current-versions [project]
  (->>  (concat (:dependencies project)
                ((comp :dependencies :dev :profiles) project))
        (map (partial take 2))
        flatten
        (apply assoc {})))

(def softwares
  [['org.clojure/clojure
    "Clojure"
    "http://clojure.org"]
   ['org.clojure/clojurescript
    "ClojureScript"
    "https://github.com/clojure/clojurescript"]
   ['com.datomic/datomic-free
    "Datomic"
    "http://datomic.com"]
   ['ring-server/ring-server
    "Ring"
    "https://github.com/ring-clojure/ring"]
   ['com.cemerick/friend
    "Friend"
    "https://github.com/cemerick/friend"]
   ['om/om
    "Om"
    "https://github.com/swannodette/om"]
   ['prismatic/om-tools
    "om-tools"
    "https://github.com/Prismatic/om-tools"]
   ['org.clojure/core.async
    "core.async"
    "https://github.com/clojure/core.async"]
   ['sablono/sablono
    "Ŝablono"
    "https://github.com/r0man/sablono"]
   ['compojure/compojure
    "Compojure"
    "https://github.com/weavejester/compojure"]
   ['lib-noir/lib-noir
    "lib-noir"
    "https://github.com/noir-clojure/lib-noir"]
   ['hiccup/hiccup
    "Hiccup"
    "https://github.com/weavejester/hiccup"]
   ['liberator/liberator
    "Liberator"
    "http://clojure-liberator.github.io/liberator/"]
   ['secretary/secretary
    "Secretary"
    "https://github.com/gf3/secretary"]
   ['clj-time/clj-time
    "clj-time"
    "https://github.com/clj-time/clj-time"]
   ['com.taoensso/timbre
    "Timbre"
    "https://github.com/ptaoussanis/timbre"]])

(s/defn software-dependencies :- [{(s/required-key :name) s/Str
                                   (s/required-key :url) s/Str
                                   (s/required-key :version) s/Str}]
  [project]
  (let [current (current-versions project)]
    (map (fn [[k name url]]
           {:name name
            :url url
            :version (k current)})
         softwares)))
