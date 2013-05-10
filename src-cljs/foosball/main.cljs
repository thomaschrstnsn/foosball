(ns foosball.main
;  (:require [foosball.tetris :as tetris])
  )

(defn log [& items]
  (.log js/console (apply str (interpose ", " items))))

(defn ^:export hello [arg]
  (js/alert (str "hey: " arg)))
