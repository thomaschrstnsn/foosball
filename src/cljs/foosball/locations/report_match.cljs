(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

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

(defn- render-players-select [id players & [selected]]
  [:select.form-control {:id id :name id :default-value (or selected "nil")}
   [:option {:value "nil" :disabled "disabled"} "Pick a player"]
   (map (fn [{:keys [id name]}]
          [:option {:value id} name])
        players)])

(defn- render-team-controls [kw team-num {:keys [player1 player2 score]} players]
  (let [prefix  (name kw)
        idp1    (str prefix "player" 1)
        idp2    (str prefix "player" 2)
        idscore (str prefix "score")]
    [:div.col-lg-5.well.well-lg
     [:h2 (str "Team " team-num ":")]
     [:div.form-group
      [:label.control-label.col-lg-4 {:for idp1} (str "Player " 1)]
      [:div.controls.col-lg-8 (render-players-select idp1 players player1)]]

     [:div.form-group
      [:label.control-label.col-lg-4 {:for idp2} (str "Player " 2)]
      [:div.controls.col-lg-8 (render-players-select idp2 players player2)]]

     [:div.form-group
      [:label.control-label.col-lg-4 {:for idscore} "Score"]
      [:div.controls.col-lg-8
       [:input.form-control {:id idscore :name idscore :type "number" :placeholder "0"
                             :min "0" :max "11" :value score}]]]]))

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
            _ (debug report-match)]
        (html
         [:div
          [:h1 "Report Match Result"]
          [:p.lead
           "A match winner is the first team to reach ten goals while atleast two goals ahead of the opposing team."
           [:br]
           "In case of tie-break, report 11-9 or 9-11."]

          [:div.form-horizontal
           [:div.form-group.col-lg-12
            (render-team-controls :team1 1 nil active-players)
            [:div.col-lg-2]
            (render-team-controls :team2 2 nil active-players)]

           (render-match-date (:matchdate report-match))

           [:div.form-group.col-lg-12
            [:div.form-group.col-lg-5.pull-right
             [:button.btn.btn-primary.btn-lg.btn-block
              {:type "submit" :value "Report"} "Report Match Result " [:span.glyphicon.glyphicon-ok]]]]]])))))

(defn render [app]
  (if (:auth app)
    (om/build report-match-component app)
    (spinner)))
