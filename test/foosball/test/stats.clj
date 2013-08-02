(ns foosball.test.stats
  (:use midje.sweet
        foosball.statistics.core
        foosball.statistics.ratings
        foosball.statistics.team-player))

(facts "about determining winners"
       (let [winning-team     {:score 10 :player1 ...wp1... :player2 ...wp2...}
             losing-team      {:score  1 :player1 ...lp1... :player2 ...lp2...}
             expected-winners #{...wp1... ...wp2...}
             expected-losers  #{...lp1... ...lp2...}]
         (fact "it works"
               (determine-winner {:team1 winning-team
                                  :team2 losing-team
                                  :something ...anything...}) => {:team1 winning-team
                                                                  :team2 losing-team
                                                                  :something ...anything...
                                                                  :winners expected-winners
                                                                  :losers  expected-losers
                                                                  :score-delta [[expected-winners 9]
                                                                                [expected-losers -9]]})))

(def example-matches
  [{:id 333, :matchdate #inst "2013-04-11T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Knud Erik", :player1 "Lisse", :id 5555},
    :team2 {:score 7, :player2 "Thomas", :player1 "Anders", :id 6666}}
   {:id 222, :matchdate #inst "2013-04-12T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Thomas", :player1 "Maria", :id 3333},
    :team2 {:score 2, :player2 "Lisse", :player1 "Anders", :id 4444}}
   {:id 111, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Lisse", :player1 "Thomas", :id 1111},
    :team2 {:score 8, :player2 "Maria", :player1 "Anders", :id 2222}}
   {:id 444, :matchdate #inst "2013-04-15T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Anders", :player1 "Knud Erik", :id 7777},
    :team2 {:score 6, :player2 "Lisse", :player1 "Maria", :id 8888}}])

(facts "about statistics when applied to example matches"
       (let [players-expected
             [{:player "Thomas",   :wins 2, :losses 1, :total 3, :win-perc 200/3, :loss-perc 100/3 :score-delta  7}
              {:player "Lisse",    :wins 2, :losses 2, :total 4, :win-perc 50N,   :loss-perc 50N   :score-delta -7}
              {:player "Anders",   :wins 1, :losses 3, :total 4, :win-perc 25N,   :loss-perc 75N   :score-delta -9}
              {:player "Maria",    :wins 1, :losses 2, :total 3, :win-perc 100/3, :loss-perc 200/3 :score-delta 2}
              {:player "Knud Erik",:wins 2, :losses 0, :total 2, :win-perc 100,   :loss-perc 0     :score-delta 7}]
             teams-expected
             [{:team #{"Lisse" "Knud Erik"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  3}
              {:team #{"Anders" "Maria"},    :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -2}
              {:team #{"Thomas" "Lisse"},    :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  2}
              {:team #{"Anders" "Knud Erik"} :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  4}
              {:team #{"Thomas" "Anders"},   :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -3}
              {:team #{"Lisse" "Anders"},    :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -8}
              {:team #{"Thomas" "Maria"},    :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  8}
              {:team #{"Lisse" "Maria"},     :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -4}]]
         (fact "it should calculate player statistics as expected"
               (calculate-player-stats example-matches) => players-expected)
         (fact "it should calculate team statistics as expected"
               (calculate-team-stats example-matches) => teams-expected)))

(facts "about ratings when applied to example matches"
       (let [all-players ["Anders" "Knud Erik" "Lisse" "Maria" "Thomas"]]
         (fact "it should calculate as expected"
               (calculate-ratings all-players example-matches) => {"Anders" 1468.883293697148,
                                                                   "Knud Erik" 1532.883293697148,
                                                                   "Lisse" 1498.5273111823453,
                                                                   "Maria" 1483.7061014233586,
                                                                   "Thomas" 1516.0}
               (calculate-ratings ["Lisse"] example-matches) => {"Lisse" 1498.5273111823453}
               (calculate-ratings [] example-matches) => {})
         (fact "it should work with a player without matches"
               (calculate-ratings ["non-existing"] example-matches) => {"non-existing" 1500})))

(facts "about player log when applied to example matches"
       (let [{:keys [logs]} (ratings-with-log ["Thomas"] example-matches)
             thomas-logs    (->> logs (filter (fn [l] (= "Thomas" (:player l)))) vec)]
         (fact "it should match the example log for Thomas"
               (count thomas-logs) => 3
               (map :new-rating thomas-logs) => (just [1484.0 (roughly 1500.29 0.009) 1516.0]))))

(facts "about player form when applied to example matches"
       (let [{:keys [logs]} (ratings-with-log ["Thomas"] example-matches)]
         (fact "it should calculate as expected"
               (calculate-current-form-for-player logs 5 "Thomas")     => [false true true]
               (calculate-current-form-for-player logs 2 "Thomas")     => [true true]
               (calculate-current-form-for-player logs 1 "Thomas")     => [true]
               (calculate-current-form-for-player logs 5 "Anders")     => [false false false true]
               (calculate-current-form-for-player logs 5 "Lisse")      => [true false true false]
               (calculate-current-form-for-player logs 5 "Knud Erik")  => [true true]
               (calculate-current-form-for-player logs 5 "Maria")      => [true false false])))

(facts "about possible-matchups"
       (fact "the number of results is bound to the size of the input"
             (possible-matchups (range 4)) => (just (repeat 3 anything))
             (possible-matchups (range 5)) => (just (repeat 15 anything))
             (possible-matchups (range 6)) => (just (repeat 45 anything))
             (possible-matchups (range 7)) => (just (repeat 105 anything))))
