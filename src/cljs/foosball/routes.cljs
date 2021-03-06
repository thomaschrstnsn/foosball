(ns foosball.routes
  (:require-macros [foosball.macros :refer [identity-map]])
  (:require [secretary.core :as secretary :include-macros true :refer [defroute]]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]
            [foosball.console :refer-macros [debug debug-js]])
  (:import goog.History goog.history.EventType))

(defn navigate-to [fragment]
  (set! (.-hash js/location) fragment))

;; defonce'd to not interfere with figwheel reloading
(defonce history
  (let [history   (History.)
        listen-fn (fn [e] (secretary/dispatch! (.-token e)))]
    (goog.events/listen history EventType/NAVIGATE listen-fn)
    (doto history (.setEnabled true))
    (debug "history listener has been setup")))

(defn init! []
  (let [req-location-chan (chan)
        set-active-menu   (fn [menu-id & args]
                            (put! req-location-chan {:id menu-id :args args}))]
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

    (defroute admin-path "/admin" []
      (set-active-menu :location/admin))

    (defroute report-match-path "/report/match" [query-params]
      (let [{:strs [t1p1 t1p2 t2p1 t2p2]} query-params]
        (set-active-menu :location/report-match (identity-map t1p1 t1p2 t2p1 t2p2))))

    (defroute about-path "/about" []
      (set-active-menu :location/about))

    (defroute "*" []
      (navigate-to (home-path)))

    (secretary/dispatch! (.substring window.location.hash 1))

    ;; hierarchy
    (let [home-location  {:id    :location/home
                          :text  "Foosball"
                          :route (home-path)}
          menu-locations [{:id    :location/matchup
                           :text  "Matchup"
                           :route (matchup-path)
                           :login-required? true}
                          {:id    :location/report-match
                           :text  "Report Match Result"
                           :route (report-match-path)
                           :login-required? true}
                          {:text  "Statistics"
                           :items [{:id    :location/player-statistics
                                    :text  "Player Statistics"
                                    :route (player-statistics-path)}
                                   {:id    :location/team-statistics
                                    :text  "Team Statistics"
                                    :route (team-statistics-path)}
                                   {:seperator :sure-thing}
                                   {:id    :location/player-log
                                    :text  "Player Log"
                                    :route (players-log-path)}]}
                          {:id    :location/matches
                           :text  "Played Matches"
                           :route (matches-path)}
                          {:id    :location/admin
                           :text  "Admin"
                           :route (admin-path)
                           :admin-required? true}
                          {:id    :location/about
                           :text  "About"
                           :route (about-path)}]]
      {:home-location     home-location
       :menu-locations    menu-locations
       :req-location-chan req-location-chan})))
