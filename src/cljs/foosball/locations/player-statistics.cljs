(ns foosball.locations.player-statistics
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]))

(defn handle [app v]
  (data/ensure-player-data app)
  (data/go-get-data! {:server-url "/api/ratings/player-stats"
                      :app app
                      :key :player-statistics
                      :server-data-transform data/add-uuid-key})
  (loc/set-location app (:id v)))

(defn render [{:keys [player-statistics player-lookup]}]
  (if-not (and player-lookup player-statistics)
    (spinner)
    (let [position-col {:heading "Position"
                        :fn      :position
                        :printer (fn [p] (str p "."))
                        :sort-fn identity}
          columns [position-col
                   {:heading "Player"
                    :fn      :player
                    :printer (partial f/format-player-link player-lookup)
                    :sort-fn identity
                    :align   :left}
                   {:heading "Wins"
                    :fn      :wins
                    :sort-fn identity}
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
                    :sort-fn identity}
                   {:heading [:div "Inactive" [:br] "Days/Matches"]
                    :key     :inactive-days
                    :fn      #(select-keys % [:days-since-latest-match :matches-after-last])
                    :printer (fn [{:keys [days-since-latest-match matches-after-last]}]
                               (list days-since-latest-match "/" matches-after-last))
                    :align   :left}
                   {:heading "Form"
                    :fn      :form
                    :printer (partial f/style-form true false)
                    :align   :left}
                   {:heading "Rating"
                    :fn      :rating
                    :printer f/style-rating
                    :sort-fn identity}]]
      (om/build table/table
                {:rows          player-statistics
                 :columns       columns
                 :caption       [:h1 "Player Statistics"]
                 :default-align :right
                 :class         ["table-hover" "table-bordered"]}
                {:state {:sort {:column position-col
                                :dir    :asc}}}))))
