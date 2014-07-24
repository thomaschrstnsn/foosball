(ns foosball.locations
  (:require [sablono.core :as html :refer-macros [html]]
            [foosball.location :refer [set-location]]
            [foosball.locations.player-statistics :as player-statistics]
            [foosball.locations.team-statistics :as team-statistics]
            [foosball.locations.home :as home]
            [foosball.locations.player-log :as player-log]
            [foosball.locations.matches :as matches]
            [foosball.locations.matchup :as matchup]
            [foosball.locations.report-match :as report-match]
            [foosball.locations.about :as about]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defmulti request-new-location (fn [app req] (-> @app :current-location)))

(defmulti handle-new-location (fn [app req] (:id req)))

(def lookup {:location/player-statistics {:handle player-statistics/handle
                                          :render player-statistics/render}
             :location/team-statistics   {:handle team-statistics/handle
                                          :render team-statistics/render}
             :location/home              {:handle home/handle
                                          :render home/render}
             :location/player-log        {:handle player-log/handle
                                          :render player-log/render}
             :location/matches           {:handle matches/handle
                                          :render matches/render}
             :location/about             {:handle about/handle
                                          :render about/render}
             :location/matchup           {:handle matchup/handle
                                          :render matchup/render}
             :location/report-match      {:handle report-match/handle
                                          :render report-match/render}})

(defmethod request-new-location :default [app new]
  (handle-new-location app new))

(defmethod handle-new-location :default [app {:keys [id] :as v}]
  (let [func (:handle (get lookup id
                           {:handle (fn [_ _] (set-location app id))}))]
    (func app v)))

(defmulti render-location (fn [{:keys [current-location]}] current-location))

(defmethod render-location :default [{:keys [current-location] :as app}]
  (let [func (get-in lookup [current-location :render])]
    (if func
      (func app)
      (list [:h1 (str current-location)]
            [:p  "Med dig"]))))
