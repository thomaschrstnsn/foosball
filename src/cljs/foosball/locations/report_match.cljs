(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [foosball.macros :refer [identity-map]])
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
            [foosball.routes :as routes]
            [foosball.spinners :refer [spinner]]
            [foosball.validation.match :as vm]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [sablono.core :as html :refer-macros [html]]))

(defn handle [app {:keys [id args]}]
  (let [unauthorized? (and (:auth @app)
                           (not (-> @app :auth :logged-in?)))]
    (if-not unauthorized?
      (do
        (data/go-get-data!
         {:server-url "/api/players"
          :app app
          :key :players
          :satisfied-with-existing-app-data? true
          :on-data-complete (fn [data]
                              (let [empty-team          {:player1 nil :player2 nil :score nil :valid-score? true}
                                    {:keys [t1p1 t1p2
                                            t2p1 t2p2]} (first args)
                                    player-from-data (fn [p] (let [player-id (uuid/make-uuid-from p)]
                                                              (->> data
                                                                   (filter (fn [{:keys [id]}] (= id player-id)))
                                                                   first)))
                                    team-from-players   (fn [p1 p2]
                                                          (when (and p1 p2)
                                                            {:player1 (player-from-data p1)
                                                             :player2 (player-from-data p2)}))
                                    team1               (merge empty-team (team-from-players t1p1 t1p2))
                                    team2               (merge empty-team (team-from-players t2p1 t2p2))]
                                (om/transact! app [:match-report] (fn [rm] (merge rm
                                                                                 (identity-map team1 team2)
                                                                                 {:matchdate (tc/now)})))))})

        (loc/set-location app id))
      (.back js/history))))

(defn selected-players [team other-team]
  (let [players (for [t      [team other-team]
                      player [:player1 :player2]]
                  (get t player))]
    (->> players
         (filter identity)
         set)))

(defcomponentk team-score-component
  [[:data :as team]
   owner
   [:opts score-ch]]
  (render [_]
    (html
     [:div.form-group (when-not (:valid-score? team) {:class "has-error"})
      [:label.control-label.col-lg-4 "Score"]
      [:div.controls.col-lg-8
       (om/build e/editable team {:opts {:value-fn  (comp str :score)
                                         :change-ch score-ch
                                         :placeholder "0"
                                         :input-props {:type "number"}}})]])))

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

(defcomponentk team-player-component
  [[:data selected-player team other-team active-players player-num]
   owner
   [:opts change-ch]]
  (render [_]
    (html
     [:div.form-group
      [:label.control-label.col-lg-4 (str "Player " player-num)]
      [:div.controls.col-lg-8
       (render-players-select selected-player team other-team active-players change-ch)]])))

(defn team-selector [team-num]
  (keyword (str "team" team-num)))

(defn player-selector [player-num]
  (keyword (str "player" player-num)))

(defn other-team [this-team]
  (let [difference (disj #{:team1 :team2} this-team)]
    (when (= 1 (count difference))
      (first difference))))

(defn- render-team-controls [{:keys [match-report active-players] :as app}
                             team-num
                             {:keys [score player1 player2] :as chans}]
  (let [team-sel       (team-selector team-num)
        other-team-sel (other-team team-sel)
        team           (team-sel match-report)
        other-team     (other-team-sel match-report)
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
               (get match-report (team-selector team-num))
               {:opts {:score-ch score}})]))

(defcomponentk matchdate-component
  [[:data :as matchdate]
   owner
   [:opts change-ch]]
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
                                                             :today (tc/now)})})]]])))

(defn valid-score? [this other]
  (:team1score (vm/validate-scores [this other])))

(defn update-score! [match-report team score other-score]
  (let [valid-score? (valid-score? score other-score)]
    (om/transact! match-report team (fn [v] (merge v
                                                  {:score score}
                                                  {:valid-score? valid-score?})))))

(defn handle-score-update [match-report [team _] [type val]]
  (when (= :foosball.editable/blur type)
    (if-let [score (c/->int val)]
      (let [other (other-team team)
            other-score (get-in @match-report [other :score])]
        (update-score! match-report team score other-score)
        (when other-score
          (update-score! match-report other other-score score))))))

(defn handle-player-update [match-report path player]
  (om/update! match-report path player))

(defn handle-matchdate-update [match-report path js-date]
  (let [matchdate (dc/to-date js-date)]
    (om/update! match-report path matchdate)))

(defn valid-team-to-report? [{:keys [player1 player2 valid-score?]}]
  (and player1 player2 valid-score?))

(defn valid-report? [{:keys [team1 team2 matchdate] :as match-report}]
  (and (valid-team-to-report? team1)
       (valid-team-to-report? team2)
       matchdate))

(defn submit-report! [match-report]
  (let [id (uuid/make-random-uuid)
        {:keys [team1 team2 matchdate]} @match-report
        report {:team1 team1
                :team2 team2
                :matchdate matchdate
                :id id
                :status :pending}]
    (go
      (try
        (let [player-id-fixer (fn [{:keys [id]}] id)
              score-fixer     (fn [score] (or score 0))
              fixed-report    (-> report
                                  (update-in [:team1 :player1] player-id-fixer)
                                  (update-in [:team1 :player2] player-id-fixer)
                                  (update-in [:team1 :score] score-fixer)
                                  (update-in [:team2 :player1] player-id-fixer)
                                  (update-in [:team2 :player2] player-id-fixer)
                                  (update-in [:team2 :score] score-fixer))
              resp            (data/throw-err (<! (data/post! (str "/api/match/" id) fixed-report)))]
          (assert (= 201 (:status resp)))
          (om/transact! match-report
                        (fn [mr] (-> mr
                                    (merge {:submitting nil})
                                    (merge {:status :ok})
                                    (update-in [:team1 :score] (constantly nil))
                                    (update-in [:team2 :score] (constantly nil))))))
        (catch js/Object e
          (do (debug :error e)
              (om/transact! match-report (fn [mr] (merge mr {:status     :error
                                                            :submitting nil})))))))
    (om/transact! match-report
                  (fn [mr] (merge mr {:submitting report})))))

(defcomponentk match-report-component
  [[:data active-players match-report :as app]
   owner]
  (init-state [_]
    (letfn [(team-chans-fact [] {:score (chan)
                                 :player1 (chan)
                                 :player2 (chan)})]
      {:team1     (team-chans-fact)
       :team2     (team-chans-fact)
       :matchdate (chan)}))

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
            #{:score} (handle-score-update match-report path v)
            #{:player1 :player2} (handle-player-update match-report path v)
            #{:matchdate} (handle-matchdate-update match-report path v)
            nil)
          (recur)))))

  (render-state [_ {:keys [team1 team2 matchdate]}]
    (let [submitting (:submitting match-report)
          status     (:status match-report)
          _    (debug "players" (map (fn [p] (str "'" (:name p) "'"))
                                     (selected-players (:team1 match-report)
                                                       (:team2 match-report))))
          __   (debug "scores" (map (fn [kw] (get-in match-report [kw :score])) [:team1 :team2]))
          ___  (debug "matchdate" (-> match-report :matchdate d/->str))
          ____ (debug "submitting" submitting)
          render-team (partial render-team-controls app)
          enabled? (and (not submitting)
                        (valid-report? match-report))]
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
                   (:matchdate match-report)
                   {:opts {:change-ch matchdate}})

         [:div.form-group.col-lg-12
          (when status
            (let [[alert-class
                   content] (if (= :ok status)
                   [:alert-success
                    [:p
                     [:strong "Success! "]
                     [:span "Result reported successfully. "]
                     [:a {:href (routes/player-statistics-path)} "Player statistics"]]]
                   [:alert-danger
                    [:p [:strong "Error! "] [:span "Oh oh! Something went wrong."]]])]
              [:div.form-group.col-lg-7.alert
               {:class alert-class}
               content]))

          [:div.form-group.col-lg-5.pull-right
           [:button.btn.btn-primary.btn-lg.btn-block
            (merge {:on-click (fn [e] (submit-report! match-report))}
                   (when-not enabled? {:disabled "disabled"}))
            "Report Match Result " (if submitting
                                     [:span.glyphicon.glyphicon-time]
                                     [:span.glyphicon.glyphicon-ok])]]]]]))))

(defn render [app]
  (if (:auth app)
    (om/build match-report-component app
              {:fn (fn [{:keys [match-report players]}]
                     {:match-report   match-report
                      :active-players (filterv :active players)})})
      (spinner)))
