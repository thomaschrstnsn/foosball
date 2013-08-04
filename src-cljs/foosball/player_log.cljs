(ns foosball.player-log
  (:use-macros [dommy.macros :only [sel1]])
  (:require [dommy.core :as dommy]))

(defn auto-submit-playerlog []
  (let [input (sel1 [:#playerid])
        form  (sel1 [:form])]
    (dommy/listen! input :change #(.submit form))))
