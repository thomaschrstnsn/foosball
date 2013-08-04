(ns foosball.navbar
  (:use-macros [dommy.macros :only [sel]])
  (:require [dommy.core :as dommy]
            [foosball.dom-utils :as u]))

(defn navbar-elements []
  (sel [".nav" :li]))

(defn current-navbar-element []
  (->> (navbar-elements)
       (filter (fn [e] (-> (dommy/html e)
                          (.indexOf (u/current-path))
                          (>= 0))))
       first))

(defn set-active-navbar-element! []
  (let [el (current-navbar-element)]
    (when el
      (dommy/add-class! el :active))))
