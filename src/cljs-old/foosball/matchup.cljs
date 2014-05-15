(ns foosball.matchup
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:use [jayq.core :only [$]])
  (:require [cljs.core.async :as async :refer [<!]]
            [foosball.dom-utils :as u]))

(defn enable-submit-on-enough-players []
  (let [[select-el select-chan] (u/elem-and-chan :#playerids :change)
        button                  (sel1 :button)]
    (go (loop []
          (let [event           (<! select-chan)
                target          (.-target event)
                selected        ($ "option:selected" target)
                enough-players? (<= 4 (count selected))]
            (if enough-players?
              (u/enable-el button)
              (u/disable-el button))
            (recur))))))
