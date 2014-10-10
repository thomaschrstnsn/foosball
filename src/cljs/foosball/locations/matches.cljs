(ns foosball.locations.matches
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.date :as d]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]))

(defn handle [app v]
  (data/ensure-player-data app)
  (data/go-get-data! {:server-url "/api/matches"
                      :app app
                      :key :matches
                      :server-data-transform data/add-uuid-key
                      :set-to-nil-until-complete true})
  (loc/set-location app (:id v)))

(defn render [{:keys [matches player-lookup]}]
  (if-not (and matches player-lookup)
    (spinner)
    (let [players-from-team (fn [{:keys [player1 player2]}] (mapv :id [player1 player2]))
          date-column {:heading "Date played"
                       :fn      :matchdate
                       :printer d/->str
                       :align   :left
                       :sort-fn identity}
          columns  [date-column
                    {:heading "Team 1"
                     :key     :team1-players
                     :fn      (comp players-from-team :team1)
                     :align   :left
                     :printer (partial f/format-team-links player-lookup)}
                    {:heading "Score"
                     :key     :team1-score
                     :fn      (comp :score :team1)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Team 2"
                     :key     :team2-players
                     :fn      (comp players-from-team :team2)
                     :align   :left
                     :printer (partial f/format-team-links player-lookup)}
                    {:heading "Score"
                     :key     :team2-score
                     :fn      (comp :score :team2)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Reported by"
                     :key     :reported-by
                     :fn      (comp :id :reported-by)
                     :printer (partial f/format-player-link player-lookup)
                     :align   :left}]]
      (om/build table/table
                {:rows          matches
                 :columns       columns
                 :caption       [:h1 "Played Matches"]
                 :default-align :right
                 :class         ["table-hover" "table-bordered"]}
                {:state {:sort {:column date-column
                                :dir    :desc}}}))))
