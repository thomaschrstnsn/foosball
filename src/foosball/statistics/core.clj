(ns foosball.statistics.core
  (:require [clojure.set :as set]))

(defn determine-winner [{:keys [team1 team2] :as match}]
  (let [t1score  (:score team1)
        t2score  (:score team2)
        t1delta  (- t1score t2score)
        t2delta  (- t2score t1score)
        [t1 t2]  (map (fn [{:keys [player1 player2]}] #{player1 player2}) [team1 team2])
        [winners
         losers] (if (> t1score t2score)
                   [t1 t2]
                   [t2 t1])]
    (merge {:winners winners
            :losers  losers
            :players (set (concat winners losers))
            :score-delta [[t1 t1delta] [t2 t2delta]]}
           match)))

(defn players-from-matches [matches]
  (->> matches
       (map (fn [{:keys [players]}] players))
       (apply set/union)))

(defn teams-from-matches [matches]
  (->> matches
       (reduce (fn [acc m] (conj acc (:winners m) (:losers m))) [])
       set))

(defn players-with-matches-by-date [comp-fn date matches]
  (->> matches (filter (fn [{:keys [matchdate]}] (comp-fn matchdate date)))
       players-from-matches))

(defn- player-contained? [player col]
  (contains? col player))

(defn player-is-winner? [player match]
  (player-contained? player (:winners match)))

(defn player-is-loser? [player match]
  (player-contained? player (:losers match)))
