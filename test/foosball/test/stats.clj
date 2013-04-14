(ns foosball.test.stats
  (:use midje.sweet foosball.views.stats)
  (:use [midje.util :only [testable-privates]]))

(testable-privates foosball.views.stats determine-winner calculate-player-stats calculate-team-stats)

(facts "about determining winners"
       (let [winning-team {:score 10 :player1 ...wp1... :player2 ...wp2...}
             losing-team  {:score  1 :player1 ...lp1... :player2 ...lp2...}]
         (fact "it works"
               (determine-winner {:team1 winning-team
                                  :team2 losing-team
                                  :something ...anything...}) => {:team1 winning-team
                                                                  :team2 losing-team
                                                                  :something ...anything...
                                                                  :winners #{...wp1... ...wp2...}
                                                                  :losers  #{...lp1... ...lp2...}})))

(facts "an example for determining statistics"
       (let [matches
             [{:id 17592186045430, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
               :team1 {:score 10, :player2 "Lisse", :player1 "Thomas", :id 17592186045428},
               :team2 {:score 5, :player2 "Maria", :player1 "Anders", :id 17592186045429}}
              {:id 17592186045442, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
               :team1 {:score 10, :player2 "Thomas", :player1 "Maria", :id 17592186045440},
               :team2 {:score 5, :player2 "Lisse", :player1 "Anders", :id 17592186045441}}
              {:id 17592186045438, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
               :team1 {:score 10, :player2 "Knud Erik", :player1 "Lisse", :id 17592186045436},
               :team2 {:score 5, :player2 "Thomas", :player1 "Anders", :id 17592186045437}}
              {:id 17592186045446, :matchdate #inst "2013-04-13T22:00:00.000-00:00",
               :team1 {:score 10, :player2 "Anders", :player1 "Knud Erik", :id 17592186045444},
               :team2 {:score 5, :player2 "Lisse", :player1 "Maria", :id 17592186045445}}]
             players-expected
             [{:player "Thomas",   :wins 2, :losses 1, :total 3, :win-perc 200/3, :loss-perc 100/3}
              {:player "Lisse",    :wins 2, :losses 2, :total 4, :win-perc 50N,   :loss-perc 50N}
              {:player "Anders",   :wins 1, :losses 3, :total 4, :win-perc 25N,   :loss-perc 75N}
              {:player "Maria",    :wins 1, :losses 2, :total 3, :win-perc 100/3, :loss-perc 200/3}
              {:player "Knud Erik",:wins 2, :losses 0, :total 2, :win-perc 100,   :loss-perc 0}]
             teams-expected
             [{:team #{"Lisse" "Knud Erik"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0}
              {:team #{"Anders" "Maria"}, :wins 0, :losses 1, :total 1, :win-perc 0, :loss-perc 100}
              {:team #{"Thomas" "Lisse"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0}
              {:team #{"Anders" "Knud Erik"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0}
              {:team #{"Thomas" "Anders"}, :wins 0, :losses 1, :total 1, :win-perc 0, :loss-perc 100}
              {:team #{"Lisse" "Anders"}, :wins 0, :losses 1, :total 1, :win-perc 0, :loss-perc 100}
              {:team #{"Thomas" "Maria"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0}
              {:team #{"Lisse" "Maria"}, :wins 0, :losses 1, :total 1, :win-perc 0, :loss-perc 100}]]
         (fact "about player statistics"
               (calculate-player-stats matches) => players-expected)
         (fact "about team statistics"
               (calculate-team-stats matches) => teams-expected)))
