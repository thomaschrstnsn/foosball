(ns foosball.navbar
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [dommy.core :as dommy]
            [foosball.dom-utils :as u]))

(defn current-navbar-element []
  (when-not (= (u/current-path) "/")
    (->> (sel [".nav" :li])
         (filter (fn [e] (-> (dommy/html e)
                            (.indexOf (u/current-path))
                            (>= 0))))
         first)))

(defn set-active-navbar-element! []
  (let [el (current-navbar-element)]
    (when el
      (dommy/add-class! el :active))))
