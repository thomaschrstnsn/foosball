(ns foosball.routes
  (:require [secretary.core :as secretary :include-macros true :refer [defroute]]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]
            [foosball.console :refer-macros [debug]])
  (:import goog.History
           goog.history.EventType))

(defn navigate-to [fragment]
  (set! (.-hash js/location) fragment))

(defn init! []
  (let [req-location-chan (chan)]
    (letfn [(set-active-menu [menu-id & args] (put! req-location-chan {:id menu-id :args args}))]
      (secretary/set-config! :prefix "#")

      (defroute home-path "/" []
        (set-active-menu :location/home))

      (defroute player-statistics-path "/statistics/player" []
        (set-active-menu :location/player-statistics))

      (defroute team-statistics-path "/statistics/team" []
        (set-active-menu :location/team-statistics))

      (defroute player-log-path "/player/log/:player-id" [player-id]
        (set-active-menu :location/player-log player-id))

      (defroute players-log-path "/player/log/" []
        (set-active-menu :location/player-log))

      (defroute matches-path "/matches" []
        (set-active-menu :location/matches))

      (defroute matchup-path "/matchup" []
        (set-active-menu :location/matchup))

      (defroute about-path "/about" []
        (set-active-menu :location/about))

      (defroute "*" []
        (navigate-to (home-path)))

      (let [h (History.)]
        (goog.events/listen h EventType/NAVIGATE
                            (fn [e] (secretary/dispatch! (.-token e))))
        (doto h (.setEnabled true)))

      ;; hierarchy
      (let [home-location  {:id    :location/home
                            :text  "Foosball"
                            :route (home-path)}
            menu-locations [{:id    :location/matchup
                             :text  "Matchup"
                             :route (matchup-path)
                             :login-required? true}
                            {:id    :location/player-statistics
                             :text  "Player Statistics"
                             :route (player-statistics-path)}
                            {:id    :location/team-statistics
                             :text  "Team Statistics"
                             :route (team-statistics-path)}
                            {:id    :location/player-log
                             :text  "Player Log"
                             :route (players-log-path)}
                            {:id    :location/matches
                             :text  "Played Matches"
                             :route (matches-path)}
                            {:id    :location/about
                             :text  "About"
                             :route (about-path)}]]
        {:home-location     home-location
         :menu-locations    menu-locations
         :req-location-chan req-location-chan}))))
