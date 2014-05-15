(ns foosball.navbar
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [dommy.core :as dommy]
            [foosball.dom-utils :as u]))

(defn set-active-navbar-by-id
  "Adds class active to the element with given id.
   Also supports dropdown menus, such that if element has a parent li.dropdown it is also set active."
  [id]
  (let [el              (sel1 id)
        dropdown-parent (->> (dommy/ancestor-nodes el)
                             (filter (fn [e] (dommy/has-class? e :dropdown)))
                             first)
        set-active!     (fn [e] (when e (dommy/add-class! e :active)))]
    (set-active! el)
    (set-active! dropdown-parent)))
