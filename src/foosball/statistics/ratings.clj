(ns foosball.statistics.ratings
  (:use [foosball.statistics.core]
        [foosball.statistics.elo]
        [foosball.util :only [symbols-as-map less-than-or-equal?]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]))

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

(defn- updated-rating-and-log-for-player-in-match [ratings match winners losers active-player-bonus player]
  (let [winner?     (player-is-winner? player  match)
        team        (if winner?        winners losers)
        opponents   (if winner?        losers  winners)
        actual      (if winner?        1.0     0.0)
        team-mate   (->> (set/difference team #{player}) first)
        expected    (expected-sum-for-player ratings [player team-mate] opponents)
        new-rating  (+ active-player-bonus (updated-rating-for-player ratings player actual expected))]
    {:rating {player new-rating}
     :log    {:log-type   :played-match
              :player     player
              :matchdate  (:matchdate match)
              :team-mate  team-mate
              :opponents  opponents
              :expected   expected
              :win?       winner?
              :delta      (- new-rating (ratings player))
              :new-rating new-rating}}))

(def ^:const default-active-player-bonus 2.0)

(defn updated-rating-and-log-for-inactive-player [ratings match inactivity-penalty player]
  (let [current-rating (get ratings player)
        new-rating     (- current-rating inactivity-penalty)]
    {:log {:log-type   :inactivity
           :player     player
           :matchdate  (:matchdate match)
           :delta      (- inactivity-penalty)
           :new-rating new-rating
           :inactivity 1}
     :rating {player new-rating}}))

(defn inactive-players-rating [active all-players ratings match]
  (let [active-player-bonus          default-active-player-bonus
        bonus-total                  (* (count active) active-player-bonus)
        players-with-positive-rating (->> all-players
                                          (filter (fn [p] (pos? (- (ratings p) bonus-total))))
                                          set)
        inactive                     (set/difference players-with-positive-rating active)]
    (if (empty? inactive)
      {:active-player-bonus 0 :inactive-ratings {}}
      (let [penalty-per-player  (/ bonus-total (count inactive))
            update-fn           (partial updated-rating-and-log-for-inactive-player ratings match penalty-per-player)
            inactive-ratings-and-logs (map update-fn inactive)
            inactive-ratings    (->> inactive-ratings-and-logs
                                     (map :rating)
                                     (apply merge))]
        {:active-player-bonus active-player-bonus
         :inactive-ratings inactive-ratings
         :inactive-logs    (map :log inactive-ratings-and-logs)}))))

(defn update-ratings-from-match [all-players
                                 won-matches
                                 {:keys [ratings logs players]}
                                 {:keys [winners losers players matchdate] :as match}]
  (let [match-players        players
        inactive-players     (players-with-matches-by-date less-than-or-equal? matchdate won-matches)
        {:keys
         [active-player-bonus
          inactive-ratings
          inactive-logs]}    (inactive-players-rating match-players inactive-players ratings match)
        updating-fn          (partial updated-rating-and-log-for-player-in-match
                                      ratings match winners losers active-player-bonus)
        new-ratings-and-logs (map updating-fn match-players)]
    {:ratings (apply  merge ratings inactive-ratings (map :rating new-ratings-and-logs))
     :logs    (concat logs  (map :log new-ratings-and-logs) inactive-logs)}))

(def ^:private initial-rating 1500)

(defn ratings-with-log [players matches]
  (let [won-matches      (map determine-winner matches)
        all-players      (set/union (players-from-matches won-matches) (set players))
        initial          (->> all-players
                              (map (fn [p] {p initial-rating}))
                              (apply merge))]
    (reduce (partial update-ratings-from-match all-players won-matches)
            {:ratings initial :logs []}
            won-matches)))

(defn calculate-ratings [players matches]
  (-> (ratings-with-log players matches)
      :ratings
      (select-keys players)))

(defn calculate-reduced-log-for-player [player matches]
  (let [all-logs    (-> (ratings-with-log [player] matches) :logs)
        player-logs (filter (comp (partial = player) :player) all-logs)]
    (->> player-logs
         (partition-by :log-type)
         (mapcat (fn [[{:keys [log-type]} :as logs]]
                   (if-not (= log-type :inactivity)
                     logs
                     [(reduce (fn [log1 log2]
                                (let [existing-dates (get log1 :matchdates #{})
                                      logs-dates     (-> (map :matchdate [log1 log2]) set)
                                      updated-dates  (set/union existing-dates logs-dates)
                                      delta          (->> [log1 log2]
                                                          (map :delta)
                                                          (apply +))
                                      inactivities   (->> [log1 log2]
                                                          (map :inactivity)
                                                          (apply +))]
                                  (merge log2 {:matchdates updated-dates
                                               :delta      delta
                                               :inactivity inactivities})))
                              logs)]))))))

(defn calculate-current-form-for-player [logs number-of-matches player]
  (->> logs
       (filter (comp (partial = :played-match) :log-type))
       (filter (comp (partial = player)        :player))
       reverse
       (take number-of-matches)
       reverse
       (map :win?)))

(defn teams-from-players [players]
  (let [ps (set players)]
    (->> (combo/combinations players 2)
         (map set)
         (map (fn [t1] (set [t1 (set/difference ps t1)]))))))

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
  (let [current-ratings   (calculate-ratings (map :name selected-players) matches)
        player-names      (map :name selected-players)
        possible-matchups (possible-matchups player-names)]
    (map (partial matchup-with-rating current-ratings) possible-matchups)))
