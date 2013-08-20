(ns foosball.views.matchup
  (:use  hiccup.form ring.util.response
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [foosball.util])
  (:require [foosball.statistics.ratings :as ratings]
            [foosball.statistics.team-player :as stats])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- format-matchup-percentage [p]
  (format-value p
                :printer (partial format-percentage 3)
                :class? #(not= (double 0) (double %))))

(defn- headers-matchup []
  [:thead
   [:tr
    [:th [:div.text-right "Team 1"]]
    [:th [:div.text-center "Rating ±"]]
    [:th ""]
    [:th [:div.text-center "Expected %"]]
    [:th ""]
    [:th [:div.text-center "Rating ±"]]
    [:th "Team 2"]]])

(defn- render-matchup [players {:keys [pos-players neg-players expected-diff pos-rating-diff neg-rating-diff]}]
  [:tr
   [:td [:div.text-right  (render-team players pos-players)]]
   [:td [:div.text-center (format-rating pos-rating-diff)]]
   [:td [:div.text-center (when (pos? expected-diff) [:span.glyphicon.glyphicon-arrow-left])]]
   [:td [:div.text-center (format-matchup-percentage (* 100 expected-diff))]]
   [:td [:div.text-center (when (neg? expected-diff) [:span.glyphicon.glyphicon-arrow-right])]]
   [:td [:div.text-center (format-rating neg-rating-diff)]]
   [:td (render-team players neg-players)]])

(defn- players-select [players selected]
  (let [number-of-players-to-show (min 16 (count players))]
    [:select.form-control {:id "playerids" :name "playerids[]" :multiple "multiple" :size number-of-players-to-show}
     (->> players
          (map (fn [{:keys [id name]}]
                 [:option (merge {:value id}
                                 (when (contains? selected id) {:selected "selected"}))
                  name])))]))

(defn page [players matches & [selected-playerids]]
  (let [players-stats   (->> matches stats/calculate-player-stats
                             (map (fn [{:keys [player] :as player-struct}] {player player-struct}))
                             (apply merge))
        active-players  (filter :active players)
        sorted-players  (->> active-players
                             (sort-by (fn [{:keys [name]}] (->> name (get players-stats) :total)))
                             reverse)
        playerid-set    (set selected-playerids)
        enough-players? (<= 4 (count playerid-set))]
    (html5
     [:h1 "Design the perfect matchup"]
     [:p.lead
      "Pick the players available for a match (atleast four)." [:br]
      "Then see the possible combinations of teams and their expected win/lose ratios."]
     [:form.form-horizontal {:action "/matchup" :method "POST"}
      [:div.control-group
       [:label.control-label.col-lg-2 {:for "playerids"} "Select atleast four players"]
       [:div.col-lg-2
        (players-select sorted-players playerid-set)]]
      [:div.control-group.col-lg-3
       [:button.btn.btn-primary.btn-lg.btn-block (merge  {:type "submit" :value "show"}
                                                            (when-not enough-players? {:disabled "disabled"}))
        "Show possible matchups"]]]
     (when enough-players?
       (let [selected-players (filter (fn [{:keys [id]}] (contains? playerid-set id)) active-players)
             matchups (ratings/calculate-matchup matches selected-players)]
         [:table.table.table-hover
          [:caption [:h1 "Matchups"]]
          (headers-matchup)
          [:tbody
           (->> matchups
                (sort-by :expected-sortable)
                (map (partial render-matchup active-players)))]])))))
