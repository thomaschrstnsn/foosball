(ns foosball.statistics.ratings
  (:use foosball.statistics.elo foosball.statistics.core)
  (:use [clojure.set :only [difference]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- expected-sum-for-player [ratings player team-mate opponents]
  (let [ra  (->> [player team-mate] (map ratings) (reduce + 0))
        rb2 (->> opponents (map ratings) (reduce + 0))]
    (->> (expected-score (/ ra 2) (/ rb2 2)) first)))

(defn- updated-rating-for-player [ratings player actual expected]
  (updated-rating (ratings player) actual expected))

(defn- updated-rating-and-log-for-player-in-match [ratings match winners losers player]
  (let [winner?     (player-is-winner? player  match)
        team        (if winner?        winners losers)
        opponents   (if winner?        losers  winners)
        actual      (if winner?        1.0     0.0)
        team-mate   (->> (difference team #{player}) first)
        expected    (expected-sum-for-player ratings player team-mate opponents)
        new-rating  (updated-rating-for-player ratings player actual expected)]
    {:rating {player new-rating}
     :log    {:player     player
              :matchdate  (:matchdate match)
              :team-mate  team-mate
              :opponents  opponents
              :expected   expected
              :win?       winner?
              :delta      (- new-rating (ratings player))
              :new-rating new-rating}}))

(defn update-ratings-from-match [{:keys [ratings logs]}
                                 {:keys [winners losers] :as match}]
  (let [players              (concat winners losers)
        new-ratings-and-logs (map (partial updated-rating-and-log-for-player-in-match ratings match winners losers) players)]
    {:ratings (apply   merge ratings (map :rating new-ratings-and-logs))
     :logs    (concat  logs          (map :log    new-ratings-and-logs))}))

(def ^:private initial-rating 1500)

(defn ratings-with-log [matches]
  (let [won-matches (map determine-winner matches)
        players     (players-from-matches won-matches)
        initial     (->> players (map (fn [p] {p initial-rating})) (apply merge))]
    (reduce update-ratings-from-match {:ratings initial :logs []} won-matches)))

(defn calculate-ratings [matches]
  (:ratings (ratings-with-log matches)))

(defn calculate-current-form-for-player [logs number-of-matches player]
  (->> logs
       (filter (fn [l] (= (:player l) player)))
       reverse
       (take number-of-matches)
       reverse
       (map :win?)))
