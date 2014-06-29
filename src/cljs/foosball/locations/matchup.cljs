(ns foosball.locations.matchup
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]))

(defn handle [app v]
  (if (-> @app :auth :logged-in?)
    (loc/set-location app (:id v))
    (.back js/history)))

(defn render [app]
  [:h1 "Hello matchup!"])
