(ns foosball.statistics.ratings
  (:use foosball.statistics.elo foosball.statistics.core)
  (:use [clojure.set :only [difference]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [clojure.math.combinatorics :as combo]))

(defn- expected-sum-for-teams [ratings heroes opponents]
  (let [rate-team (fn [players] (->> players
                                    (map ratings)
                                    (reduce + 0)
                                    ((fn [r] (/ r 2)))))
        rating-a    (rate-team heroes)
        rating-b    (rate-team opponents)]
    (expected-score rating-a rating-b)))

(defn- expected-sum-for-player [ratings heroes opponents]
  (->> (expected-sum-for-teams ratings heroes opponents)
       first))

(defn- updated-rating-for-player [ratings player actual expected]
  (updated-rating (ratings player) actual expected))

(defn- updated-rating-and-log-for-player-in-match [ratings match winners losers player]
  (let [winner?     (player-is-winner? player  match)
        team        (if winner?        winners losers)
        opponents   (if winner?        losers  winners)
        actual      (if winner?        1.0     0.0)
        team-mate   (->> (difference team #{player}) first)
        expected    (expected-sum-for-player ratings [player team-mate] opponents)
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

(defn teams-from-players [players]
  (let [ps (set players)]
    (->> (combo/combinations players 2)
         (map set)
         (map (fn [t1] (set [t1 (difference ps t1)]))))))

(defn possible-matchups [players]
  (->> (combo/combinations players 4)
       (map teams-from-players)
       (reduce concat)
       set))

(defn matchup-with-rating [ratings teams]
  (let [[heroes foes]                (vec teams)
        [hero-expected foe-expected] (expected-sum-for-teams ratings heroes foes)
        expected-diff                (- hero-expected foe-expected)
        selected-hero                (first heroes)
        selected-foe                 (first foes)
        hero-new-rating              (updated-rating-for-player ratings selected-hero 1.0 hero-expected)
        foe-new-rating               (updated-rating-for-player ratings selected-foe  1.0 foe-expected)
        hero-rating-diff             (- hero-new-rating (ratings selected-hero))
        foe-rating-diff              (- foe-new-rating  (ratings selected-foe))]
    {:pos-expected hero-expected
     :pos-players heroes
     :neg-expected foe-expected
     :neg-players foes
     :expected-diff expected-diff
     :expected-sortable (Math/abs expected-diff)
     :pos-rating-diff hero-rating-diff
     :neg-rating-diff foe-rating-diff}))

(defn calculate-matchup [matches selected-players]
  (let [current-ratings   (calculate-ratings matches)
        player-names      (map :name selected-players)
        possible-matchups (possible-matchups player-names)]
    (map (partial matchup-with-rating current-ratings) possible-matchups)))
