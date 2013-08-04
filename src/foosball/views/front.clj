(ns foosball.views.front
  (:use [hiccup.element :only [link-to]]
        [hiccup.page :only [html5]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util :as util]
            [foosball.statistics.ratings :as ratings]
            [foosball.statistics.team-player :as player]))

(defn- render-player [players index p]
  [:tr
   [:td [:h3 (inc index)]]
   [:td [:h3 (->> p :player (util/get-player-by-name players) util/link-to-player-log)]]
   [:td [:h3 (map #(util/format-value % :printer {true "W" false "L"} :class? nil :checker true? :container-tag :span) (:form p))]]
   [:td [:h3 (util/format-value (:rating p) :printer util/format-rating :class? nil :checker (partial < 1500))]]])

(defn- nav-button [url label]
  [:div.col-lg-6
      [:a.btn.btn-info.btn-large.btn-block {:href url} label]])

(defn page [players matches]
  (html5
   (util/auto-refresh-page)
   [:div.jumbotron
    [:h1 "Foosball"]
    [:h2 "Keeps track of results, ratings and players for foosball matches."]
    [:div.row
     (nav-button "/stats/players" "See all player's ratings")
     (nav-button "/matchup"       "Matchup players for a match")]
    [:br]
    [:div.row
     (nav-button "/report/match"  "Report the result of a match")
     (nav-button "/matches"       "See all played matches' results")]]
   [:table.table.table-hover.table-condensed
    [:caption [:h1 "Current leaderboard"]]
    [:thead
     [:tr
      [:th [:h3 "Position"]]
      [:th [:h3 "Player"]]
      [:th [:h3 "Form"]]
      [:th [:h3 "Rating"]]]
     [:tbody
      (let [stats             (player/calculate-player-stats matches)
            log-and-ratings   (ratings/ratings-with-log players matches)
            ratings           (:ratings log-and-ratings)
            logs              (:logs log-and-ratings)
            stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                     (merge stat
                                            {:rating (ratings player)}
                                            {:form   (ratings/calculate-current-form-for-player logs 5 player)}))
                                   stats)]
        (->> stats-and-ratings
             (sort-by :rating)
             (reverse)
             (take 5)
             (map (partial render-player players) (range))))]]]))
