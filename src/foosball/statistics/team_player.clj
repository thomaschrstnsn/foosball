(ns foosball.statistics.team-player
  (:use [foosball.statistics core]))

(defn- player-stats [matches player]
  (let [wins        (->> (filter (partial player-is-winner? player) matches) count)
        losses      (->> (filter (partial player-is-loser?  player) matches) count)
        total       (+ wins losses)
        win-perc    (* (/ wins total) 100)
        loss-perc   (* (/ losses total) 100)
        score-delta (->>  (map :score-delta matches) (reduce concat)
                          (filter (fn [[t s]] (contains? t player))) (map (fn [[t s]] s))
                          (reduce + 0))]
    {:player player :wins wins :losses losses :total total
     :win-perc win-perc :loss-perc loss-perc :score-delta score-delta}))


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
        players     (players-from-matches won-matches)
        ratings     ()]
    (map (partial player-stats won-matches) players)))

(defn calculate-team-stats [matches]
  (let [won-matches (map determine-winner matches)
        teams       (teams-from-matches won-matches)]
    (map (partial team-stats won-matches) teams)))
