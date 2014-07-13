(ns foosball.locations.player-log
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.routes :as routes]
            [cljs-uuid-utils :as uuid]))

(defn handle [app {:keys [args] :as v}]
  (let [player-id (first args)]
    (when-not (@app :players)
      (data/go-update-data! "/api/players" app :players))
    (om/update! app :player-log-player ())
    (om/update! app :players nil)
    (om/update! app :player-log nil)
    (when player-id
      (do
        (om/update! app :player-log-player (uuid/make-uuid-from player-id))
        (data/go-update-data! (str "/api/ratings/log/" player-id) app :player-log)))
    (data/go-update-data! "/api/players" app :players)
    (loc/set-location app (:id v))))

(defn render [{:keys [player-log player-log-player players]}]
  (when players
    (let [player (when (and player-log-player player-log)
                   (->> players
                        (filter (fn [{:keys [id]}] (= id player-log-player)))
                        first))]
      (list
       [:div.form-group.col-lg-3
        [:select.form-control {:value (if player
                                        (str (:id player))
                                        "default")
                               :on-change (fn [e]
                                            (routes/navigate-to
                                             (routes/player-log-path {:player-id (-> e .-target .-value)})))}
         [:option {:value "default" :disabled "disabled"} "Active players"]
         (map (fn [{:keys [id name]}] [:option {:value id} name]) players)]]
       (when player-log
         (let [columns      [{:heading "Match date"
                              :key     :matchdate
                              :printer (fn [d] (when d (f/format-date d)))}
                             {:heading "Team mate"
                              :key     :team-mate
                              :printer (fn [tm] (when tm (f/format-player-link players tm)))}
                             {:heading "Opponents"
                              :key     :opponents
                              :printer (fn [ops] (when ops (f/format-team-links players ops)))}
                             {:heading "Expected"
                              :key     :expected
                              :printer (fn [v] (when v (f/style-match-percentage true (* 100 v))))}
                             {:heading "Actual"
                              :key     :win?
                              :printer (fn [v] (f/style-value v
                                                             :class?  nil
                                                             :printer {true "Won" false "Lost"}
                                                             :checker true?))}
                             {:heading "Inactive matches"
                              :key     :inactivity}
                             {:heading "Diff rating"
                              :key     :delta
                              :printer (fn [v] (when v (f/style-value v :printer f/format-rating)))}
                             {:heading "New rating"
                              :key     :new-rating
                              :printer f/style-rating}]
               row-class-fn (fn [{:keys [log-type]}] (when (= :inactivity log-type) "danger"))]
           (om/build table/table player-log {:opts {:columns       columns
                                                    :caption       [:h1 (str "Player Log: " (:name player))]
                                                    :default-align :right
                                                    :class         ["table-hover" "table-bordered"]
                                                    :row-class-fn  row-class-fn}})))))))
