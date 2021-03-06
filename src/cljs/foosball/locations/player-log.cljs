(ns foosball.locations.player-log
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.date :as d]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.routes :as routes]
            [foosball.spinners :refer [spinner]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [cljs-uuid-utils :as uuid]))

(defn handle [app {:keys [args] :as v}]
  (let [player-id (first args)]
    (data/ensure-player-data app)
    (om/update! app :player-log-player nil)
    (om/update! app :player-log nil)
    (when player-id
      (do
        (om/update! app :player-log-player (uuid/make-uuid-from player-id))
        (data/go-get-data! {:server-url (str "/api/ratings/log/" player-id)
                           :app app
                           :key :player-log})))
    (loc/set-location app (:id v))))

(defn render [{:keys [player-log player-log-player player-lookup players]}]
  (if-not player-lookup
    (spinner)
    (let [player (when (and player-log-player player-log)
                   (get player-lookup player-log-player))]
      (list
       [:h1 "Player Log"]
       [:p.lead "Pick a player to see the played matches of this player."]
       [:div.form-group.col-lg-3
        [:select.form-control {:value (if player
                                        (str (:id player))
                                        "default")
                               :on-change (fn [e]
                                            (routes/navigate-to
                                             (routes/player-log-path {:player-id (-> e .-target .-value)})))}
         [:option {:value "default" :disabled "disabled"} "Active players"]
         (map (fn [{:keys [id name]}] [:option {:value id} name]) players)]]
       (when player-log-player
         (if-not player-log
           (spinner)
           (let [columns      [{:heading "Match date"
                                :fn      :matchdate
                                :printer (fn [d] (when d (d/->str d)))}
                               {:heading "Team mate"
                                :fn      :team-mate
                                :printer (fn [tm] (when tm (f/format-player-link player-lookup tm)))}
                               {:heading "Opponents"
                                :fn      :opponents
                                :printer (fn [ops] (when ops (f/format-team-links player-lookup ops)))}
                               {:heading "Expected"
                                :fn      :expected
                                :printer (fn [v] (when v (f/style-match-percentage true (* 100 v))))}
                               {:heading "Actual"
                                :fn      :win?
                                :printer (fn [v]
                                           (f/style-value
                                            v
                                            :class?  nil
                                            :printer {true "Won" false "Lost"}
                                            :checker true?))}
                               {:heading "Inactive matches"
                                :fn      :inactivity}
                               {:heading "Diff rating"
                                :fn      :delta
                                :printer (fn [v] (when v (f/style-value v :printer f/format-rating)))}
                               {:heading "New rating"
                                :fn      :new-rating
                                :printer f/style-rating}]
                 row-class-fn (fn [{:keys [log-type]}] (when (= :inactivity log-type) "danger"))]
             (om/build table/table
                       {:rows          player-log
                        :columns       columns
                        :caption       [:h1 (str "Player Log: " (:name player))]
                        :default-align :right
                        :class         ["table-hover" "table-bordered"]
                        :row-class-fn  row-class-fn}))))))))
