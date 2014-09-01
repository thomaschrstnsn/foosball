(ns foosball.locations.home
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.routes :as routes]
            [foosball.auth :as auth]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn handle [app v]
  (om/update! app :leaderboard nil)
  (when-not (@app :players)
    (data/go-update-data! "/api/players" app :players))
  (data/go-update-data! "/api/ratings/leaderboard/5" app :leaderboard)
  (loc/set-location app (:id v)))

(defn- nav-button [url label]
  [:div.col-lg-6
      [:a.btn.btn-info.btn-lg.btn-block {:href url} label]])

(defn render [{:keys [leaderboard players auth]}]
  (when players
    (list
     [:div.jumbotron
      [:h1 "Foosball"]
      [:h2 "Keeps track of results, ratings and players for foosball matches."]
      (if (:logged-in? auth)
        [:div
         [:div.row
          (nav-button (routes/player-statistics-path) "See ratings for all players")
          (nav-button (routes/matchup-path)           "Matchup players for a match")]
         [:br]
         [:div.row
          (nav-button (routes/report-match-path) "Report the result of a match")
          (nav-button (routes/matches-path)      "See results of all played  matches")]]
        [:div.row
         (nav-button (routes/player-statistics-path) "See ratings for all players")
         (when auth [:div.col-lg-6 (auth/login-form auth
                                                    :button-class "btn-lg btn-block"
                                                    :button-text "Login or create a new player")])])]
     (let [columns [{:heading "Position"
                     :key :position
                     :printer (fn [p] (str p "."))}
                    {:heading "Player"
                     :key :player/name
                     :printer (partial f/format-player-link players)
                     :align :left}
                    {:heading "Form"
                     :key :form
                     :printer (partial f/style-form :won :lost)
                     :align :left}
                    {:heading "Rating"
                     :key :rating
                     :printer f/style-rating}]]
       (om/build table/table leaderboard
                 {:opts {:columns           columns
                         :caption           [:h1 "Current leaderboard"]
                         :default-container :h3
                         :class             ["table-hover" "table-condensed"]}})))))
