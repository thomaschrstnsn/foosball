(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [cljs-uuid-utils :as uuid]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle [app v]
  (let [unauthorized? (and (:auth @app)
                           (not (-> @app :auth :logged-in?)))]
    (if-not unauthorized?
      (do
        (when-not (@app :players)
          (data/go-update-data! "/api/players" app :players))
        (let [empty-team {:player1 nil :player2 nil :score nil}]
          (om/update! app [:report-match] {:team1 empty-team
                                           :team2 empty-team
                                           :matchdate (js/Date.)}))
        (loc/set-location app (:id v)))
      (.back js/history))))

(defn selected-players [report-match]
  (let [paths (for [team   [:team1 :team2]
                    player [:player1 :player2]]
                [team player])]
    (->> paths
         (map (fn [p] (get-in report-match p)))
         (filter identity)
         set)))

(defn- render-players-select [report-match players selected-player-path]
  (let [selected-player  (get-in report-match selected-player-path)
        other-selected   (disj (selected-players report-match) selected-player)
        player-lookup    (group-by :id players)
        possible-options (->> players
                              (filter (complement (partial contains? other-selected))))]
    [:select.form-control
     {:default-value (or (:id selected-player) "nil")
      :on-change (fn [e]
                   (let [player-id (-> e .-target .-value uuid/make-uuid-from)
                         player    (first (get player-lookup player-id))]
                     (om/update! report-match selected-player-path player)))}
     [:option {:value "nil" :disabled "disabled"} "Pick a player"]
     (map (fn [{:keys [id name]}]
            [:option {:value id} name])
          possible-options)]))

(defn render-team-score [{:keys [score] :as team} ]
  [:div.form-group
      [:label.control-label.col-lg-4 "Score"]
      [:div.controls.col-lg-8
       [:input.form-control {:type "number" :placeholder "0"
                             :min "0" :max "11" :value score}]]])

(defn render-team-player [report-match players team-path num]
  (let [player-selector (keyword (str "player" num))
        team            (get-in report-match team-path)]
    [:div.form-group
     [:label.control-label.col-lg-4 (str "Player " num)]
     [:div.controls.col-lg-8
      (render-players-select report-match players (conj team-path player-selector))]]))

(defn- render-team-controls [report-match players team-num]
  (let [team-selector (keyword (str "team" team-num))
        team          (team-selector report-match)
        render-player (partial render-team-player report-match players [team-selector])]
    [:div.col-lg-5.well.well-lg
     [:h2 (str "Team " team-num ":")]
     (render-player 1)
     (render-player 2)
     (render-team-score team)]))

(defn render-match-date [matchdate]
  (let [formated-date (f/format-date matchdate)]
    [:div.form-group.col-lg-12
     [:div.form-group.pull-right.col-lg-6
      [:label.control-label.col-lg-6 {:for "matchdate"} "Date played"]
      [:div.controls.col-lg-5
       [:input.input-medium.form-control {:id "matchdate"
                                          :type "date"
                                          :value formated-date}]]]]))

(defn report-match-component [{:keys [players report-match] :as app} owner]
  (reify
    om/IRender
    (render [_]
      (let [active-players (filterv :active players)
            _ (debug report-match)
            render-team (partial render-team-controls report-match active-players)]
        (html
         [:div
          [:h1 "Report Match Result"]
          [:p.lead
           "A match winner is the first team to reach ten goals while atleast two goals ahead of the opposing team."
           [:br]
           "In case of tie-break, report 11-9 or 9-11."]

          [:div.form-horizontal
           [:div.form-group.col-lg-12
            (render-team 1)
            [:div.col-lg-2]
            (render-team 2)]

           (render-match-date (:matchdate report-match))

           [:div.form-group.col-lg-12
            [:div.form-group.col-lg-5.pull-right
             [:button.btn.btn-primary.btn-lg.btn-block
              {:type "submit" :value "Report"} "Report Match Result " [:span.glyphicon.glyphicon-ok]]]]]])))))

(defn render [app]
  (if (:auth app)
    (om/build report-match-component app)
    (spinner)))
