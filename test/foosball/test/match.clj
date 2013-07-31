(ns foosball.test.match
  (:use midje.sweet foosball.views.match foosball.validation.match)
  (:use [midje.util :only [testable-privates]])
  (:require [foosball.util :as util]))

(testable-privates foosball.views.match
                   filter-active-players)

(testable-privates foosball.validation.match
                   validate-scores
                   validate-players
                   pick-players)

(facts "about the form"
       (fact "it works with just a players arguments"
             (form ...players...) => truthy)
       (let [team1  {:player1 ...t1p1... :player2 ...t1p2... :score ...t1score...}
             team2  {:player1 ...t2p1... :player2 ...t2p2... :score ...t2score...}
             report {:team1 team1
                     :team2 team2
                     :matchdate ...date...}]
         (fact "it works with a report structure"
               (form ...players... report) => truthy
               (provided
                (util/format-date ...date...) => ...datestring...
                (#'foosball.views.match/team-controls :team1 1 team1 ...active-players...) => ...team1controls...
                (#'foosball.views.match/team-controls :team2 2 team2 ...active-players...) => ...team2controls...
                (#'foosball.views.match/filter-active-players ...players...) => ...active-players...))))
