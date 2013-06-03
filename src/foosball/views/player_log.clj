(ns foosball.views.player-log
  (:use [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [foosball.statistics team-player ratings])
  (:use [foosball.util])
  (:require [clojure.string :as string]))

(defn- players-select [id players & [selected]]
  [:select.input-medium.submit-on-select {:id id :name id}
   [:option {:value "nil" :disabled "disabled" :selected "selected"} "Select a player"]
   (->> players
        (map (fn [{:keys [id name]}]
               [:option (merge {:value id}
                               (when (= id selected) {:selected "selected"}))
                name])))])

(defn- render-log [l]
  [:tr
   [:td (format-datetime (:matchdate l))]
   [:td (:team-mate l)]
   [:td (string/join ", " (:opponents l))]
   [:td (format-percentage (* 100 (:expected l)))]
   [:td {:class (if (:win? l) "text-success" "text-error")} (if (:win? l) "Won" "Lost")]
   [:td (format-rating (:delta l))]
   [:td (format-rating (:new-rating l))]])

(defn player-table [matches player]
  [:table.table.table-hover.table-bordered
   [:caption [:h2 (str "Played Matches: " player) ]]
   [:thead [:tr
            [:th "Match date"]
            [:th "Team mate"]
            [:th "Opponents"]
            [:th "Expected"]
            [:th "Actual"]
            [:th "Diff rating"]
            [:th "New Rating"]]
    [:tbody
     (->> matches
          ratings-with-log
          :logs
          (filter (fn [l] (= player (:player l))))
          reverse
          (map render-log))]]])

(defn player-log-page [matches players selected-playerid]
  (let [playerid (parse-id selected-playerid)
        player   (->> players (filter (fn [p] (= (:id p) playerid))) first)]
    (html5
     [:h1 (str "Player Log")]
     [:form {:action "/rating/player" :method "GET"}
      [:div.input-append
       (players-select "playerid" players playerid)
       [:button.btn {:type "submit" :value "select"} "Select"]]]
     (when player (player-table matches (:name player))))))
