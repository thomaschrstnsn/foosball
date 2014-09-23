(ns foosball.statistics.ratings
  (:require [clojure.math.combinatorics :as combo]
            [clojure.set :as set]
            [clj-time.coerce :refer [from-date]]
            [clj-time.core :refer [in-days interval]]
            [foosball.statistics.core :refer :all]
            [foosball.statistics.elo :refer :all]
            [foosball.statistics.team-player :as player]
            [foosball.util :refer [less-than-or-equal?]]
            [foosball.entities :as e]
            [schema.core :as s]))

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
            inactive-ratings-and-logs   (map update-fn inactive)
            inactive-ratings    (->> inactive-ratings-and-logs
                                     (map :rating)
                                     (apply merge))]
        {:active-player-bonus active-player-bonus
         :inactive-ratings    inactive-ratings
         :inactive-logs       (map :log inactive-ratings-and-logs)}))))

(defn update-ratings-from-match [all-players
                                 won-matches
                                 active-players-by-matchid
                                 {:keys [ratings logs players]}
                                 {:keys [winners losers players matchdate match/id] :as match}]
  (let [match-players        players
        active-players       (active-players-by-matchid id)
        {:keys
         [active-player-bonus
          inactive-ratings
          inactive-logs]}    (inactive-players-rating match-players active-players ratings match)
        updating-fn          (partial updated-rating-and-log-for-player-in-match
                                      ratings match winners losers active-player-bonus)
        new-ratings-and-logs (map updating-fn match-players)]
    {:ratings (apply  merge ratings inactive-ratings (map :rating new-ratings-and-logs))
     :logs    (concat logs  (map :log new-ratings-and-logs) inactive-logs)}))

(def ^:private initial-rating 1500)

(defn calc-accum-active-players-by-match-id
  "Builds the accumulated set of active players as a map from match-id to set of player names.
   The input is assumed to be sorted matches by time played (earliest first)"
  [won-matches]
  (let [accum-sets (drop 1 (reductions
                            (fn [accum {:keys [players]}] (set/union players accum))
                            #{}
                            won-matches))
        seperate-maps (map (fn [{:keys [match/id]} accum] {id accum}) won-matches accum-sets)]
    (apply merge seperate-maps)))

(defn ratings-with-log [players matches]
  (let [won-matches    (map determine-winner matches)
        all-players    (set/union (players-from-matches won-matches) (->> players (map :id) set))
        active-players (calc-accum-active-players-by-match-id won-matches)
        initial        (->> all-players
                            (map (fn [p] {p initial-rating}))
                            (apply merge))]
    (merge
     (reduce (partial update-ratings-from-match all-players won-matches active-players)
             {:ratings initial :logs []}
             won-matches)
     {:won-matches won-matches})))

(defn calculate-ratings [players matches]
  (-> (ratings-with-log players matches)
      :ratings
      (select-keys (map :id players))))

(defn calculate-reduced-log-for-player [player matches]
  (let [all-logs    (-> (ratings-with-log [player] matches) :logs)
        player-id   (:id player)
        player-logs (filter (comp (partial = player-id) :player) all-logs)]
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

(defn group-by-many
  "Like group-by but assumes that f returns a collection of keys,
   the current value is stored on each key in that collection."
  [f coll]
  (persistent!
   (reduce
    (fn [ret x]
      (reduce (fn [acc k] (assoc! acc k (conj (get acc k []) x)))
              ret (f x)))
    (transient {}) coll)))

(defn calculate-form-from-matches [won-matches form-length]
  (let [matches-by-player      (group-by-many :players won-matches)
        form-matches-by-player (map (fn [[player matches]] [player (take-last form-length matches)]) matches-by-player)
        winner?-fn             (fn [player {:keys [winners]}] (contains? winners player))
        form-by-player         (map (fn [[player matches]]
                                      {player (map (partial winner?-fn player) matches)})
                                    form-matches-by-player)]
    (apply merge form-by-player)))

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
  (let [current-ratings   (calculate-ratings selected-players matches)
        possible-matchups (possible-matchups (mapv :id selected-players))]
    (map (partial matchup-with-rating current-ratings) possible-matchups)))

(s/defn leaderboard :- [{(s/required-key :position)    s/Int
                         (s/required-key :player/id)   s/Uuid
                         (s/required-key :form)        [(s/enum :won :lost)]
                         (s/required-key :rating)      s/Num}]
  [matches :- [e/Match]
   players :- [e/User]
   size    :- s/Int]
  (let [stats             (player/calculate-player-stats matches)
        log-and-ratings   (ratings-with-log players matches)
        ratings           (:ratings log-and-ratings)
        logs              (:logs log-and-ratings)
        won-matches       (:won-matches log-and-ratings)
        form-by-player    (calculate-form-from-matches won-matches 5)
        stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                 (merge stat
                                        {:rating (ratings player)}
                                        {:form   (form-by-player player)}))
                               stats)]
    (->> stats-and-ratings
         (sort-by :rating)
         (reverse)
         (take size)
         (map (fn [index player] {:position (inc index)
                                 :player/id (:player player)
                                 :form (map {true :won false :lost} (:form player))
                                 :rating (:rating player)})
              (range)))))

(defn calculate-player-stats-table [matches players]
  (let [stats             (player/calculate-player-stats matches)
        log-and-ratings   (ratings-with-log players matches)
        ratings           (:ratings log-and-ratings)
        won-matches       (:won-matches log-and-ratings)
        today             (from-date (java.util.Date.))
        forms-by-player   (calculate-form-from-matches won-matches 5)
        stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                 (merge stat
                                        {:rating                  (ratings player)
                                         :form                    (forms-by-player player)
                                         :days-since-latest-match (in-days (interval (from-date (:latest-matchdate stat))
                                                                                     today))}))
                               stats)
        with-position     (map (fn [s n] (merge s {:position (inc n)}))
                               (reverse (sort-by :rating stats-and-ratings))
                               (range))]
    with-position))
