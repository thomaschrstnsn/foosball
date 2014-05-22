(ns foosball.views.player-log
  (:require [foosball.statistics.ratings :refer :all]
            [foosball.util :as util]
            [hiccup.page :refer [html5]]))

(defn- player-option [selected {:keys [id name]}]
  [:option (merge {:value id}
                  (when (= id selected) {:selected "selected"}))
   name])

(defn- players-select [id players & [selected]]
  (let [grouped  (group-by :active players)
        active   (get grouped true)
        inactive (get grouped false)]
    [:select.form-control.submit-on-select {:id id :name id}
     [:option {:value "nil" :disabled "disabled" :selected "selected"} "Active players"]
     (map (partial player-option selected) active)
     (when inactive
       [:option {:value "nil" :disabled "disabled"} "Inactive players"])
     (when inactive
       (map (partial player-option selected) inactive))]))

(defn- render-log-matchplayed [players l]
  [:tr
   [:td (util/format-date (:matchdate l))]
   [:td (->> (:team-mate l) (util/get-player-by-name players) util/link-to-player-log)]
   [:td (->> (:opponents l)
             (map #(->> % (util/get-player-by-name players) util/link-to-player-log))
             (interpose ", "))]
   [:td (util/format-value (* 100 (:expected l))
                           :printer util/format-percentage
                           :class? (partial not= (double 50))
                           :checker (partial < 50))]
   [:td (util/format-value (:win? l)
                           :class? nil
                           :printer {true "Won" false "Lost"}
                           :checker true?)]
   [:td ""]
   [:td (util/format-value (:delta l) :printer util/format-rating)]
   [:td (util/format-value (:new-rating l)
                           :printer util/format-rating
                           :class? nil
                           :checker (partial < 1500))]])

(defn- render-log-inactivity [players l]
  [:tr.danger
   [:td ""]
   [:td ""]
   [:td ""]
   [:td ""]
   [:td ""]
   [:td.text-danger (:inactivity l)]
   [:td (util/format-value (:delta l) :printer util/format-rating)]
   [:td (util/format-value (:new-rating l) :printer util/format-rating :class? nil :checker (partial < 1500))]])

(defn- render-log [players {:keys [log-type] :as l}]
  (if (= :inactivity  log-type)
    (render-log-inactivity   players l)
    (render-log-matchplayed  players l)))

(defn player-table [matches players {:keys [name active] :as player}]
  [:table.table.table-hover
   [:caption [:h2 (str "Played Matches: " name (when-not active " (inactive)")) ]]
   [:thead [:tr
            [:th "Match date"]
            [:th "Team mate"]
            [:th "Opponents"]
            [:th "Expected"]
            [:th "Actual"]
            [:th "Inactive" [:br] "Matches"]
            [:th "Diff rating"]
            [:th "New Rating"]]
    [:tbody
     (->> (calculate-reduced-log-for-player name matches)
          reverse
          (map (partial render-log players)))]]])

(defn player-log-page [matches players selected-playerid]
  (let [playerid (when selected-playerid (util/uuid-from-string selected-playerid))
        player   (->> players (filter (comp (partial = playerid) :id)) first)]
    (html5
     [:h1 (str "Player Log")]
     [:p.lead "Pick a player to see the played matches of this player."]
     [:form#log-form {:action "/player/log" :method "GET"}
      [:div.form-group.col-lg-3 (players-select "playerid" players playerid)]]
     (when player (player-table matches players player)))))