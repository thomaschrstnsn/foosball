(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [cljs-time.coerce :as dc]
            [cljs-time.core :as tc]
            [cljs-uuid-utils :as uuid]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [foosball.convert :as c]
            [foosball.data :as data]
            [foosball.date :as d]
            [foosball.datepicker :as dp]
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
                                           :matchdate (tc/now)}))
        (loc/set-location app (:id v)))
      (.back js/history))))

(defn selected-players [team other-team]
  (let [players (for [t      [team other-team]
                      player [:player1 :player2]]
                  (get t player))]
    (->> players
         (filter identity)
         set)))

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

(defn- render-players-select [selected-player team other-team active-players change-ch]
  (let [other-selected   (disj (selected-players team other-team) selected-player)
        possible-options (->> active-players
                              (filter (complement (partial contains? other-selected))))]
    [:select.form-control
     {:value     (or (:id selected-player) "nil")
      :on-change (fn [e]
                   (let [player-lookup (group-by :id active-players)
                         player-id     (-> e .-target .-value uuid/make-uuid-from)
                         player        (first (get player-lookup player-id))]
                     (put! change-ch player)))}
     [:option {:value "nil" :disabled "disabled"} "Pick a player"]
     (map (fn [{:keys [id name]}]
            [:option {:value id} name])
          possible-options)]))

(defn team-player-component [{:keys [selected-player team other-team active-players player-num]}
                             owner
                             {:keys [change-ch]}]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.form-group
        [:label.control-label.col-lg-4 (str "Player " player-num)]
        [:div.controls.col-lg-8
         (render-players-select selected-player team other-team active-players change-ch)]]))))

(defn team-selector [team-num]
  (keyword (str "team" team-num)))

(defn player-selector [player-num]
  (keyword (str "player" player-num)))

(defn other-team [this-team]
  (let [difference (disj #{:team1 :team2} this-team)]
    (when (= 1 (count difference))
      (first difference))))

(defn- render-team-controls [{:keys [report-match active-players] :as app}
                             team-num
                             {:keys [score player1 player2] :as chans}]
  (let [team-sel       (team-selector team-num)
        other-team-sel (other-team team-sel)
        team           (team-sel report-match)
        other-team     (other-team-sel report-match)
        render-player  (fn [num change-ch]
                         (let [selected-player ((player-selector num) team)]
                           (om/build team-player-component team
                                     {:opts      {:change-ch change-ch}
                                      :react-key (str team-selector "-" num)
                                      :fn        (fn [t] {:selected-player selected-player
                                                         :team t
                                                         :other-team other-team
                                                         :active-players active-players
                                                         :player-num num})})))]
    [:div.col-lg-5.well.well-lg
     [:h2 (str "Team " team-num ":")]
     (render-player 1 player1)
     (render-player 2 player2)
     (om/build team-score-component
               (get report-match (team-selector team-num))
               {:opts {:score-ch score}})]))

(defn matchdate-component [matchdate owner {:keys [change-ch]}]
  (reify
    om/IRender
    (render [_]
      (html
       [:div.form-group.col-lg-12
        [:div.form-group.pull-right.col-lg-6
         [:label.control-label.col-lg-6 "Date played"]
         [:div.controls.col-lg-5
          (om/build dp/date-component matchdate {:opts {:value-key     :date
                                                        :max-value-key :today
                                                        :str-fn    d/->str
                                                        :change-ch change-ch}
                                                 :fn   (fn [x] {:date  x
                                                               :today (tc/now)})})]]]))))

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
  (om/update! report-match path player))

(defn handle-matchdate-update [report-match path js-date]
  (if-let [matchdate (dc/to-date-time js-date)]
    (debug "handle matchdate " matchdate)
                                        ;      (om/transact! report-match path (fn [v] ()))
    ))

(defn report-match-component [{:keys [active-players report-match] :as app} owner]
  (reify
    om/IInitState
    (init-state [_]
      (letfn [(team-chans-fact [] {:score (chan)
                                   :player1 (chan)
                                   :player2 (chan)})]
        {:team1     (team-chans-fact)
         :team2     (team-chans-fact)
         :matchdate (chan)}))

    om/IWillMount
    (will-mount [_]
      (let [team-paths (for [team [:team1 :team2]
                             chan [:score :player1 :player2]]
                         [team chan])
            all-paths  (conj team-paths [:matchdate])
            chan-to-path-map (apply merge
                                    (map (fn [p] {(om/get-state owner p) p})
                                         all-paths))]
        (go-loop []
          (let [[v c] (alts! (keys chan-to-path-map))
                path (get chan-to-path-map c)]
            (condp some (-> path)
              #{:score} (handle-score-update report-match path v)
              #{:player1 :player2} (handle-player-update report-match path v)
              #{:matchdate} (handle-matchdate-update report-match path v)
              nil)
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [team1 team2 matchdate]}]
      (let [_  (debug "players" (map (fn [p] (str "'" (:name p) "'"))
                                     (selected-players (:team1 report-match)
                                                       (:team2 report-match))))
            __ (debug "scores" (map (fn [kw] (get-in report-match [kw :score])) [:team1 :team2]))
            render-team (partial render-team-controls app)]
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

           (om/build matchdate-component
                     (:matchdate report-match)
                     {:opts {:change-ch matchdate}})

           [:div.form-group.col-lg-12
            [:div.form-group.col-lg-5.pull-right
             [:button.btn.btn-primary.btn-lg.btn-block
              {:type "submit" :value "Report"} "Report Match Result " [:span.glyphicon.glyphicon-ok]]]]]])))))

(defn render [app]
  (if (:auth app)
    (om/build report-match-component app
              {:fn (fn [{:keys [report-match players]}]
                     {:report-match   report-match
                      :active-players (filterv :active players)})})
    (spinner)))
