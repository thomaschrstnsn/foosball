(ns foosball.navbar
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [dommy.core :as dommy]
            [foosball.dom-utils :as u]))

(defn set-active-navbar-by-id [id]
  (let [el (sel1 id)]
    (when el
      (dommy/add-class! el :active))))
