(ns foosball.locations.team-statistics
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]))

(defn handle  [app v]
  (om/update! app :team-statistics nil)
  (when-not (@app :players)
    (data/go-update-data! "/api/players" app :players))
  (data/go-update-data! "/api/ratings/team-stats" app :team-statistics data/add-uuid-key)
  (loc/set-location app (:id v)))

(defn render [{:keys [team-statistics players]}]
  (when players
    (let [wins-col {:heading "Wins"
                    :fn :wins
                    :sort-fn identity}
          columns  [{:heading "Team"
                     :fn :team
                     :align :left
                     :printer (partial f/format-team-links players)}
                    wins-col
                    {:heading "Losses"
                     :fn :losses
                     :sort-fn identity}
                    {:heading "Played"
                     :fn :total
                     :sort-fn identity}
                    {:heading "Wins %"
                     :fn :win-perc
                     :printer (partial f/style-match-percentage true)
                     :sort-fn identity}
                    {:heading "Losses %"
                     :fn :loss-perc
                     :printer (partial f/style-match-percentage false)
                     :sort-fn identity}
                    {:heading "Score diff."
                     :fn :score-delta
                     :printer f/style-value
                     :sort-fn identity}]]
      (om/build table/table team-statistics {:opts {:columns       columns
                                                    :caption       [:h1 "Team Statistics"]
                                                    :default-align :right
                                                    :class         ["table-hover" "table-bordered"]}
                                             :state {:sort {:column wins-col
                                                            :dir    :desc}}}))))
