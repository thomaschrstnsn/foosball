(ns foosball.dom-utils
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:require [dommy.core :as dommy]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]))

(defn log [& items]
  (.log js/console (apply str (interpose ", " items))))

(defn expose [js-object]
  (.expose goog/debug js-object false))

(defn expose-log [js-object]
  (-> js-object expose log))

(defn current-path []
  window.location.pathname)

(defn event-chan [el event-type]
  (let [c (chan)]
    (dommy/listen! el event-type #(put! c %))
    c))

(defn elem-and-chan [sel-arg event-type]
  (let [el (sel1 sel-arg)
        ch (event-chan el event-type)]
    [el ch]))

(defn enable-el [el]
  (dommy/remove-attr! el :disabled))

(defn disable-el [el]
  (dommy/set-attr! el :disabled "disabled"))

(defn parse-int
  ([s] (parse-int s nil))
  ([s failure-value]
     (let [parsed (js/parseInt s)]
       (if (js/isNaN parsed)
         failure-value
         parsed))))
