(ns foosball.locations.player-statistics
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]))

(defn handle [app v]
  (om/update! app :player-statistics nil)
  (when-not (@app :players)
    (data/go-update-data! "/api/players" app :players))
  (data/go-update-data! "/api/ratings/player-stats" app :player-statistics)
  (loc/set-location app (:id v)))

(defn render [{:keys [player-statistics players]}]
  (when players
    (let [position-col {:heading "Position"
                        :key :position
                        :printer (fn [p] (str p "."))
                        :sort-fn identity}
          columns [position-col
                   {:heading "Player"
                    :key :player
                    :printer (partial f/format-player-link players)
                    :sort-fn identity
                    :align :left}
                   {:heading "Wins"
                    :key :wins
                    :sort-fn identity}
                   {:heading "Losses"
                    :key :losses
                    :sort-fn identity}
                   {:heading "Played"
                    :key :total
                    :sort-fn identity}
                   {:heading "Wins %"
                    :key :win-perc
                    :printer (partial f/style-match-percentage true)
                    :sort-fn identity}
                   {:heading "Losses %"
                    :key :loss-perc
                    :printer (partial f/style-match-percentage false)
                    :sort-fn identity}
                   {:heading "Score diff."
                    :key :score-delta
                    :printer f/style-value
                    :sort-fn identity}
                   {:heading [:div "Inactive" [:br] "Days/Matches"]
                    :key #(select-keys % [:days-since-latest-match :matches-after-last])
                    :printer (fn [{:keys [days-since-latest-match matches-after-last]}]
                               (list days-since-latest-match "/" matches-after-last))
                    :align :left}
                   {:heading "Form"
                    :key :form
                    :printer (partial f/style-form true false)
                    :align :left}
                   {:heading "Rating"
                    :key :rating
                    :printer f/style-rating
                    :sort-fn identity}]]
      (om/build table/table player-statistics {:opts {:columns       columns
                                                      :caption       [:h1 "Player Statistics"]
                                                      :default-align :right
                                                      :class         ["table-hover" "table-bordered"]}
                                               :state {:sort {:column position-col
                                                              :dir    :asc}}}))))
