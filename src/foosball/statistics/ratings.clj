(ns foosball.statistics.ratings
  (:use foosball.statistics.elo foosball.statistics.core)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- expected-sum-for-player [ratings player opponents]
  (let [ra  (ratings player)
        rbs (map ratings opponents)]
    (->> rbs
         (map (partial expected-score ra))
         (map first)
         (apply +))))

(defn- updated-rating-for-player [ratings player opponents actual]
  (updated-rating (ratings player) actual (expected-sum-for-player ratings player opponents)))

(defn update-ratings-from-match [ratings {:keys [winners losers] :as match}]
  (let [players (concat winners losers)]
    (->> players
         (map (fn [player]
                (let [winner?   (player-is-winner? player match)
                      opponents (if winner?        losers winners)
                      actual    (if winner?        2.0    0.0)]
                  {player (updated-rating-for-player ratings player opponents actual)})))
         ((partial apply merge ratings)))))

(def ^:private initial-rating 1500)

(defn recalculate-ratings [matches]
  (let [won-matches (->> matches (map determine-winner) (sort-by :matchdate))
        players     (players-from-matches won-matches)
        initial     (->> players (map (fn [p] {p initial-rating})) (apply merge))]
    (reduce update-ratings-from-match initial won-matches)))
