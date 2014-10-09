(ns foosball.locations.team-statistics
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]))

(defn handle  [app v]
  (data/ensure-player-data app)
  (data/go-get-data! {:server-url "/api/ratings/team-stats"
                      :app app
                      :key :team-statistics
                      :server-data-transform data/add-uuid-key
                      :set-to-nil-until-complete true})
  (loc/set-location app (:id v)))

(defn render [{:keys [team-statistics player-lookup]}]
  (if-not (and player-lookup team-statistics)
    (spinner)
    (let [wins-col {:heading "Wins"
                    :fn      :wins
                    :sort-fn identity}
          columns  [{:heading "Team"
                     :fn      :team
                     :align   :left
                     :printer (partial f/format-team-links player-lookup)}
                    wins-col
                    {:heading "Losses"
                     :fn      :losses
                     :sort-fn identity}
                    {:heading "Played"
                     :fn      :total
                     :sort-fn identity}
                    {:heading "Wins %"
                     :fn      :win-perc
                     :printer (partial f/style-match-percentage true)
                     :sort-fn identity}
                    {:heading "Losses %"
                     :fn      :loss-perc
                     :printer (partial f/style-match-percentage false)
                     :sort-fn identity}
                    {:heading "Score diff."
                     :fn      :score-delta
                     :printer f/style-value
                     :sort-fn identity}]]
      (om/build table/table
                {:rows          team-statistics
                 :columns       columns
                 :caption       [:h1 "Team Statistics"]
                 :default-align :right
                 :class         ["table-hover" "table-bordered"]}
                {:state {:sort {:column wins-col
                                :dir    :desc}}}))))
