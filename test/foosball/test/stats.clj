(ns foosball.test.stats
  (:use midje.sweet foosball.statistics.core foosball.statistics.ratings foosball.statistics.team-player))

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
  [{:id 111, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Lisse", :player1 "Thomas", :id 1111},
    :team2 {:score 8, :player2 "Maria", :player1 "Anders", :id 2222}}
   {:id 222, :matchdate #inst "2013-04-12T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Thomas", :player1 "Maria", :id 3333},
    :team2 {:score 2, :player2 "Lisse", :player1 "Anders", :id 4444}}
   {:id 333, :matchdate #inst "2013-04-11T22:00:00.000-00:00",
    :team1 {:score 10, :player2 "Knud Erik", :player1 "Lisse", :id 5555},
    :team2 {:score 7, :player2 "Thomas", :player1 "Anders", :id 6666}}
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
       (fact "it should calculate as expected"
             (calculate-ratings example-matches) => {"Anders"    1471.4228435342122,
                                                     "Knud Erik" 1531.6733612916798,
                                                     "Lisse"     1497.7516300971445,
                                                     "Maria"     1482.8648524571677,
                                                     "Thomas"    1516.278225286596}))
