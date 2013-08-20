(ns foosball.statistics.team-player
  (:use [foosball.statistics.core]
        [foosball.util]))

(defn- player-stats [matches player]
  (let [games-won          (filter (partial player-is-winner? player) matches)
        games-lost         (filter (partial player-is-loser?  player) matches)
        games-played       (concat games-won games-lost)
        latest-match       (->> games-played (sort-by :matchdate) last)
        latest-matchdate   (:matchdate latest-match)
        matches-after-last (->> matches
                                reverse
                                (take-while (partial not= latest-match))
                                (drop-while (fn [m] (or (player-is-loser?  player m)
                                                       (player-is-winner? player m))))
                                count)
        wins               (count games-won)
        losses             (count games-lost)
        total              (+ wins losses)
        win-perc           (* (/ wins total) 100)
        loss-perc          (* (/ losses total) 100)
        score-delta        (->>  (map :score-delta matches) (reduce concat)
                                 (filter (fn [[t s]] (contains? t player))) (map (fn [[t s]] s))
                                 (reduce + 0))]
    (symbols-as-map player wins losses total win-perc loss-perc score-delta latest-matchdate matches-after-last)))

(defn- team-stats [matches team]
  (let [wins      (->> (map :winners matches) (filter (partial = team)) count)
        losses    (->> (map :losers  matches) (filter (partial = team)) count)
        total     (+ wins losses)
        win-perc  (* (/ wins total) 100)
        loss-perc (* (/ losses total) 100)
        score-delta (->>  (map :score-delta matches) (reduce concat)
                          (filter (fn [[t s]] (= team t))) (map (fn [[t s]] s))
                          (reduce + 0))]
    {:team team :wins wins :losses losses :total total :win-perc win-perc :loss-perc loss-perc :score-delta score-delta}))

(defn calculate-player-stats [matches]
  (let [won-matches (map determine-winner matches)
        players     (players-from-matches won-matches)]
    (map (partial player-stats won-matches) players)))

(defn calculate-team-stats [matches]
  (let [won-matches (map determine-winner matches)
        teams       (teams-from-matches won-matches)]
    (map (partial team-stats won-matches) teams)))
