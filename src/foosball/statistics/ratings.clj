(ns foosball.statistics.ratings
  (:use foosball.statistics.elo foosball.statistics.core)
  (:use [clojure.set :only [difference]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- expected-sum-for-player [ratings player opponents]
  (let [ra  (ratings player)
        rb2 (->> opponents (map ratings) (reduce + 0))]
    (->> (expected-score ra (/ rb2 2)) first)))

(defn- updated-rating-for-player [ratings player opponents actual]
  (updated-rating (ratings player) actual (expected-sum-for-player ratings player opponents)))

(defn- updated-rating-and-log-for-player-in-match [ratings match winners losers player]
  (let [winner?     (player-is-winner? player  match)
        team        (if winner?        winners losers)
        opponents   (if winner?        losers  winners)
        actual      (if winner?        1.0     0.0)
        new-rating  (updated-rating-for-player ratings player opponents actual)
        team-mate   (->> (difference team #{player}) first)]
    {:rating {player new-rating}
     :log    {:player player
              :team-mate team-mate :opponents opponents
              :win? winner?
              :delta (- new-rating (ratings player))
              :new-rating new-rating}}))

(defn update-ratings-from-match [{:keys [ratings logs]}
                                 {:keys [winners losers] :as match}]
  (let [players              (concat winners losers)
        new-ratings-and-logs (map (partial updated-rating-and-log-for-player-in-match ratings match winners losers) players)]
    {:ratings (apply   merge ratings (map :rating new-ratings-and-logs))
     :logs    (concat  logs          (map :log    new-ratings-and-logs))}))

(def ^:private initial-rating 1500)

(defn ratings-with-log [matches]
  (let [won-matches (->> matches (map determine-winner) (sort-by :matchdate))
        players     (players-from-matches won-matches)
        initial     (->> players (map (fn [p] {p initial-rating})) (apply merge))]
    (reduce update-ratings-from-match {:ratings initial :logs []} won-matches)))

(defn calculate-ratings [matches]
  (:ratings (ratings-with-log matches)))
