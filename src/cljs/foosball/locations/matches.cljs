(ns foosball.locations.matches
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]))

(defn handle [app v]
  (om/update! app :matches nil)
  (when-not (@app :players)
    (data/go-update-data! "/api/players" app :players))
  (data/go-update-data! "/api/matches" app :matches)
  (loc/set-location app (:id v)))

(defn render [{:keys [matches players]}]
  (when players
    (let [players-from-team (fn [{:keys [player1 player2]}] [player1 player2])
          date-column {:heading "Date played"
                       :key :matchdate
                       :printer f/format-date
                       :align :left
                       :sort-fn identity}
          columns  [date-column
                    {:heading "Team 1"
                     :key (comp players-from-team :team1)
                     :align :left
                     :printer (partial f/format-team-links players)}
                    {:heading "Score"
                     :key (comp :score :team1)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Team 1"
                     :key (comp players-from-team :team2)
                     :align :left
                     :printer (partial f/format-team-links players)}
                    {:heading "Score"
                     :key (comp :score :team2)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Reported by"
                     :key :reported-by
                     :align :left}]]
      (om/build table/table matches {:opts {:columns       columns
                                            :caption       [:h1 "Played Matches"]
                                            :default-align :right
                                            :class         ["table-hover" "table-bordered"]}
                                             :state {:sort {:column date-column
                                                            :dir    :desc}}}))))
