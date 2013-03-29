(ns foosball.test.match
  (:use midje.sweet foosball.views.match)
  (:use [midje.util :only [testable-privates]])
  (:require [foosball.util :as util]))

(testable-privates foosball.views.match
                   validate-scores
                   validate-players
                   validation-error?
                   pick-players)

(facts "about validation of scores"
       (fact "team 1 scores which should be invalid"
             (validate-scores [-1 10]) => (just :team1)
             (validate-scores [12 10]) => (just :team1))
       (fact "team 2 scores which should be invalid"
             (validate-scores [10 -1]) => (just :team2)
             (validate-scores [10 12]) => (just :team2))
       (fact "scores which are invalid for both teams"
             (let [both [:team1 :team2]]
               (validate-scores [-1 -1]) => both
               (validate-scores [10 10]) => both
               (validate-scores [10  9]) => both
               (validate-scores [ 9 10]) => both
               (validate-scores [11 10]) => both
               (validate-scores [10 11]) => both
               (validate-scores [ 1  1]) => both))
       (fact "valid scores"
             (validate-scores [10  8]) => empty?
             (validate-scores [ 8 10]) => empty?
             (validate-scores [11  9]) => empty?
             (validate-scores [ 9 11]) => empty?)
       (fact "nil values are handled"
             (validate-scores [nil   8]) => [:team1]
             (validate-scores [  8 nil]) => [:team2]
             (validate-scores [nil nil]) => [:team1 :team2]))

(facts "about picking players from parsed reports"
       (fact "it should work"
             (pick-players {:team1 {:player1 1 :player2 2}
                            :team2 {:player1 3 :player2 4}}) => [1 2 3 4]))

(facts "about validating players"
       (fact "unique non-nil players are valid"
             (validate-players [1 2 3 4]) => empty?)
       (fact "nil players are invalid"
             (validate-players [nil   2   3    4]) => [:team1player1]
             (validate-players [  1 nil   3    4]) => [:team1player2]
             (validate-players [  1   2 nil    4]) => [:team2player1]
             (validate-players [  1   2   3  nil]) => [:team2player2])
       (fact "non-unique players are invalid"
             (validate-players [1 1 2 3]) => (contains [:team1player1 :team1player2] :in-any-order)
             (validate-players [1 2 2 3]) => (contains [:team1player2 :team2player1] :in-any-order)
             (validate-players [1 2 3 3]) => (contains [:team2player1 :team2player2] :in-any-order)
             (validate-players [1 2 3 1]) => (contains [:team1player1 :team2player2] :in-any-order))
       (fact "some combinations that are invalid"
             (validate-players [1 1 nil nil]) => (contains [:team1player1 :team1player2
                                                            :team2player1 :team2player2] :in-any-order)))

(facts "about validating reports"
       (fact "a valid report should have no validation errors"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => (contains {:validation-errors empty?})
               (provided
                (#'foosball.views.match/validate-players (#'foosball.views.match/pick-players report)) => []
                (#'foosball.views.match/validate-scores  (#'foosball.views.match/pick-scores  report)) => [])))
       (fact "a report with invalid players should have validation for players added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => (contains {:validation-errors {:players [...error...]}})
               (provided
                (#'foosball.views.match/validate-players (#'foosball.views.match/pick-players report)) => [...error...]
                (#'foosball.views.match/validate-scores  (#'foosball.views.match/pick-scores  report)) => [])))
       (fact "a report with invalid scores should have validation for scores added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => (contains {:validation-errors {:scores [...error...]}})
               (provided
                (#'foosball.views.match/validate-players (#'foosball.views.match/pick-players report)) => []
                (#'foosball.views.match/validate-scores  (#'foosball.views.match/pick-scores  report)) => [...error...])))
       (fact "a report with invalid scores and invalid players should have validation for scores and players added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => (contains {:validation-errors {:scores  [...score-error...]
                                                                          :players [...player-error...]}
                                                      :team1 ...team1...
                                                      :team2 ...team2...})
               (provided
                (#'foosball.views.match/validate-players (#'foosball.views.match/pick-players report)) => [...player-error...]
                (#'foosball.views.match/validate-scores  (#'foosball.views.match/pick-scores  report)) => [...score-error...]))))

(facts "about the form"
       (fact "it works with just a players arguments"
             (form ...players...) => truthy)
       (let [team1                            {:player1 ...t1p1... :player2 ...t1p2... :score ...t1score...}
             team2                            {:player1 ...t2p1... :player2 ...t2p2... :score ...t2score...}
             report-without-validation-errors {:team1 team1
                                               :team2 team2
                                               :matchdate ...date...}
             report-with-validation-errors    {:team1 team1
                                               :team2 team2
                                               :matchdate ...date...
                                               :validation-errors [...some-error...]}]
         (fact "it works with a report structure without validation-errors"
               (form ...players... report-without-validation-errors) => truthy
               (provided
                (util/format-time ...date...) => ...datestring...
                (#'foosball.views.match/team-controls :team1 1 team1 ...players... nil) => ...team1controls...
                (#'foosball.views.match/team-controls :team2 2 team2 ...players... nil) => ...team2controls...))
         (fact "it works with a report structure with validation-errors"
               (form ...players... report-with-validation-errors) => truthy
               (provided
                (util/format-time ...date...) => ...datestring...
                (#'foosball.views.match/team-controls :team1 1 team1 ...players... [...some-error...]) => ...team1controls...
                (#'foosball.views.match/team-controls :team2 2 team2 ...players... [...some-error...]) => ...team2controls...))))

(facts "about validation-error?"
       (fact "it is truthy when it should be"
             (validation-error? {:scores [:team1]} :scores :team1) => truthy)
       (fact "it is falsy when it should be"
             (validation-error? {} :scores :team1) => falsey
             (validation-error? {:scores [:team2]} :scores :team1) => falsey
             (validation-error? {:scores []} :scores :team1) => falsey
             (validation-error? {:scores nil} :scores :team1) => falsey))
