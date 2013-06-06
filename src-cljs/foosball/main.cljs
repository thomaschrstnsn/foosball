(ns foosball.main
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:require [dommy.core :as dommy]
            [clojure.browser.repl :as repl]))

(defn log [& items]
  (.log js/console (apply str (interpose ", " items))))

(defn current-path []
  window.location.pathname)

(defn navbar-elements []
  (sel [".nav" :li]))

(defn current-navbar-element []
  (->> (navbar-elements)
       (filter (fn [e] (-> (dommy/html e)
                          (.indexOf (current-path))
                          (>= 0))))
       first))

(defn set-active-navbar-element! []
  (let [el (current-navbar-element)]
    (when el
      (dommy/add-class! el :active))))

(defn ^:export page-loaded []
  ;(repl/connect "http://localhost:9000/repl")
  (set-active-navbar-element!))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
