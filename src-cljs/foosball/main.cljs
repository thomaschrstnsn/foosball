(ns foosball.main
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:require [dommy.core :as dommy]))

(defn log [& items]
  (.log js/console (apply str (interpose ", " items))))

(defn ^:export set-active-navbar! []
  (js/alert "hey: you")
  (log (->> (sel [".nav" :li])
            (map log))))
