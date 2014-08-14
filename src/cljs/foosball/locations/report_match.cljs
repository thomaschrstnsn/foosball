(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [cljs-uuid-utils :as uuid]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [foosball.convert :as c]
            [foosball.data :as data]
            [foosball.editable :as e]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]
            [foosball.validation.match :as vm]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn handle [app v]
  (let [unauthorized? (and (:auth @app)
                           (not (-> @app :auth :logged-in?)))]
    (if-not unauthorized?
      (do
        (when-not (@app :players)
          (data/go-update-data! "/api/players" app :players))
        (let [empty-team {:player1 nil :player2 nil :score nil :valid-score? true}]
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

(defn- render-players-select [report-match players selected-player-path change-ch]
  (let [selected-player  (get-in report-match selected-player-path)
        other-selected   (disj (selected-players report-match) selected-player)
        player-lookup    (group-by :id players)
        possible-options (->> players
                              (filter (complement (partial contains? other-selected))))]
    [:select.form-control
     {:value     (or (:id selected-player) "nil")
      :on-change (fn [e]
                   (let [player-id (-> e .-target .-value uuid/make-uuid-from)
                         player    (first (get player-lookup player-id))]
                     (put! change-ch player)))}
     [:option {:value "nil" :disabled "disabled"} "Pick a player"]
     (map (fn [{:keys [id name]}]
            [:option {:value id} name])
          possible-options)]))

(defn team-score-component [team owner {:keys [score-ch]}]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.form-group (when-not (:valid-score? team) {:class "has-error"})
        [:label.control-label.col-lg-4 "Score"]
        [:div.controls.col-lg-8
         (om/build e/editable team {:opts {:value-fn  (comp str :score)
                                           :change-ch score-ch
                                           :placeholder "0"
                                           :input-props {:type "number"}}})]]))))

(defn render-team-player [report-match players team-path num change-ch]
  (let [player-selector (keyword (str "player" num))
        team            (get-in report-match team-path)]
    [:div.form-group
     [:label.control-label.col-lg-4 (str "Player " num)]
     [:div.controls.col-lg-8
      (render-players-select report-match players (conj team-path player-selector) change-ch)]]))

(defn team-selector [team-num]
  (keyword (str "team" team-num)))

(defn- render-team-controls [report-match players team-num {:keys [score player1 player2] :as chans}]
  (let [team          ((team-selector team-num) report-match)
        render-player (partial render-team-player report-match players [team-selector])]
    (debug "render team" team)
    [:div.col-lg-5.well.well-lg
     [:h2 (str "Team " team-num ":")]
     (render-player 1 player1)
     (render-player 2 player2)
     (om/build team-score-component
               (get report-match (team-selector team-num))
               {:opts {:score-ch score}})]))

(defn render-match-date [matchdate]
  (let [formated-date (f/format-date matchdate)]
    [:div.form-group.col-lg-12
     [:div.form-group.pull-right.col-lg-6
      [:label.control-label.col-lg-6 {:for "matchdate"} "Date played"]
      [:div.controls.col-lg-5
       [:input.input-medium.form-control {:id "matchdate"
                                          :type "date"
                                          :value formated-date}]]]]))

(defn other-team [this-team]
  (let [difference (disj #{:team1 :team2} this-team)]
    (when (= 1 (count difference))
      (first difference))))

(defn valid-score? [this other]
  (:team1score (vm/validate-scores [this other])))

(defn update-score! [report-match team score other-score]
  (let [valid-score? (valid-score? score other-score)]
    (om/transact! report-match team (fn [v] (merge v
                                                  {:score score}
                                                  {:valid-score? valid-score?})))))

(defn handle-score-update [report-match [team _] [type val]]
  (when (= :foosball.editable/blur type)
    (if-let [score (c/->int val)]
      (let [other (other-team team)
            other-score (get-in @report-match [other :score])]
        (update-score! report-match team score other-score)
        (when other-score
          (update-score! report-match other other-score score))))))

(defn handle-player-update [report-match path player]
  (debug "handle update" path player)
  (om/update! report-match path player))

(defn report-match-component [{:keys [players report-match] :as app} owner]
  (reify
    om/IInitState
    (init-state [_]
      (letfn [(team-chans-fact [] {:score (chan)
                                   :player1 (chan)
                                   :player2 (chan)})]
              {:team1 (team-chans-fact)
               :team2 (team-chans-fact)}))

    om/IWillMount
    (will-mount [_]
      (let [chan-to-path-map (apply merge
                                    (for [team [:team1 :team2]
                                          chan [:score :player1 :player2]]
                                      {(om/get-state owner [team chan]) [team chan]}))]
        (go-loop []
          (let [[v c] (alts! (keys chan-to-path-map))
                path (get chan-to-path-map c)]
            (condp = (second path)
              :score   (handle-score-update report-match path v)
              (handle-player-update report-match path v))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [team1 team2]}]
      (let [active-players (filterv :active players)
            _  (debug (map (fn [p]  (str "'" (:name p) "'")) (selected-players report-match)))
            __ (debug (map (fn [kw] (get-in report-match [kw :score])) [:team1 :team2]))
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
            (render-team 1 team1)
            [:div.col-lg-2]
            (render-team 2 team2)]

           (render-match-date (:matchdate report-match))

           [:div.form-group.col-lg-12
            [:div.form-group.col-lg-5.pull-right
             [:button.btn.btn-primary.btn-lg.btn-block
              {:type "submit" :value "Report"} "Report Match Result " [:span.glyphicon.glyphicon-ok]]]]]])))))

(defn render [app]
  (if (:auth app)
    (om/build report-match-component app)
    (spinner)))
