(ns foosball.views.matchup
  (:use  hiccup.form ring.util.response
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [foosball.util])
  (:require [foosball.statistics.ratings :as ratings])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- players-select [players selected]
  [:div.row
   [:div.control-group
    [:select {:id "playerids" :name "playerids[]" :multiple "multiple" :size "8"}
     (->> players
          (map (fn [{:keys [id name]}]
                 [:option (merge {:value id}
                                 (when (contains? selected id) {:selected "selected"}))
                  name])))]
    [:span.help-inline "Select atleast four players"]]])

(defn- format-matchup-percentage [p wins?]
  (format-value p
                :printer format-percentage
                :class? #(not= (double 50) (double %))
                :checker (if wins?
                           (partial < 50)
                           (partial > 50))))

(defn- headers-matchup []
  [:thead
   [:tr
    [:th "Team 1"]
    [:th {:colspan 2} [:div.text-center "Versus"]]
    [:th "Team 2"]]
   [:tr
    [:th ""]
    [:th {:colspan 2} [:div.text-center "Expected win"]]
    [:th ""]]])

(defn- render-team [players team]
  (->> team
       (map #(->> % (get-player-by-name players) link-to-player-log))
       (interpose ", ")))

(defn- render-matchup [players {:keys [t1-rating t1-players t2-rating t2-players] :as input}]
  (info {:input input})
  [:tr
   [:td [:div.text-right (render-team players t1-players)]]
   [:td (format-matchup-percentage (* 100 t1-rating) true)]
   [:td [:div.text-right (format-matchup-percentage (* 100 t2-rating) true)]]
   [:td (render-team players t2-players)]])

(defn page [players & [matches selected-playerids]]
  (let [playerid-set (set selected-playerids)]
    (html5
     [:h1 "Design the perfect matchup"]
     [:p.lead
      "Pick the players available for a match (atleast four)." [:br]
      "Then see the possible combinations of teams and their expected win/lose ratios."]
     [:form.form-horizontal {:action "/matchup" :method "POST"}
      (players-select players playerid-set)
      [:div.row
       [:div.control-group
        [:button.btn.btn-primary.btn-large {:type "submit" :value "show"} "Show possible matchups"]]]]
     (when (<= 4 (count playerid-set))
       (let [selected-players (filter (fn [{:keys [id]}] (contains? playerid-set id)) players)
             matchups (ratings/calculate-matchup matches selected-players)]
         [:table.table.table-hover.table-bordered
          [:caption [:h1 "Matchups"]]
          (headers-matchup)
          [:tbody
           (map (partial render-matchup players) matchups)]])))))
