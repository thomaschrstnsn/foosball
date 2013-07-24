(ns foosball.test.validation-match
  (:use midje.sweet
        foosball.validation.match)
  (:use [midje.util :only [testable-privates]]))

(testable-privates foosball.validation.match
                   validate-scores
                   validate-players
                   pick-players)

(facts "about validation of scores"
       (fact "team 1 scores which should be invalid"
             (validate-scores [-1 10]) => {:team1score false :team2score true}
             (validate-scores [12 10]) => {:team1score false :team2score true})
       (fact "team 2 scores which should be invalid"
             (validate-scores [10 -1]) => {:team2score false :team1score true}
             (validate-scores [10 12]) => {:team2score false :team1score true})
       (fact "scores which are invalid for both teams"
             (validate-scores [-1 -1]) => {:team1score false :team2score false}
             (validate-scores [10 10]) => {:team1score false :team2score false}
             (validate-scores [10  9]) => {:team1score false :team2score false}
             (validate-scores [ 9 10]) => {:team1score false :team2score false}
             (validate-scores [11 10]) => {:team1score false :team2score false}
             (validate-scores [10 11]) => {:team1score false :team2score false}
             (validate-scores [ 1  1]) => {:team1score false :team2score false})
       (fact "valid scores"
             (validate-scores [10  8]) => {:team1score true :team2score true}
             (validate-scores [ 8 10]) => {:team1score true :team2score true}
             (validate-scores [11  9]) => {:team1score true :team2score true}
             (validate-scores [ 9 11]) => {:team1score true :team2score true})
       (fact "nil values are handled"
             (validate-scores [nil   8]) => {:team1score nil :team2score true}
             (validate-scores [  8 nil]) => {:team2score nil :team1score true}
             (validate-scores [nil nil]) => {:team1score nil :team2score nil}))

(facts "about picking players from parsed reports"
       (fact "it should work"
             (pick-players {:team1 {:player1 1 :player2 2}
                            :team2 {:player1 3 :player2 4}}) => [1 2 3 4]
             (pick-players {:team1 {}
                            :team2 {}}) => [nil nil nil nil]))

(facts "about validating players"
       (let [all-valid {:team1player1 true :team1player2 true
                        :team2player1 true :team2player2 true}]
         (fact "unique non-nil players are valid"
               (validate-players [1 2 3 4]) => all-valid)
         (fact "nil players are invalid"
               (validate-players [nil   2   3   4]) => (merge all-valid {:team1player1 nil})
               (validate-players [  1 nil   3   4]) => (merge all-valid {:team1player2 nil})
               (validate-players [  1   2 nil   4]) => (merge all-valid {:team2player1 nil})
               (validate-players [  1   2   3 nil]) => (merge all-valid {:team2player2 nil})
               (validate-players [nil nil nil nil]) => {:team1player1 nil :team1player2 nil
                                                        :team2player1 nil :team2player2 nil})
         (fact "non-unique players are invalid"
               (validate-players [1 1 2 3]) => (merge all-valid {:team1player1 false :team1player2 false})
               (validate-players [1 2 2 3]) => (merge all-valid {:team1player2 false :team2player1 false})
               (validate-players [1 2 3 3]) => (merge all-valid {:team2player1 false :team2player2 false})
               (validate-players [1 2 3 1]) => (merge all-valid {:team1player1 false :team2player2 false}))
         (fact "some combinations that are invalid"
               (validate-players [1 1 nil nil]) => {:team1player1 false :team1player2 false
                                                    :team2player1 nil   :team2player2 nil})))

(facts "about validating reports"
       (let [all-keys     [:team1player1 :team2player1 :team1player2 :team2player2 :team1score :team2score]
             same-vals-fn (fn [val] (apply assoc {} (interleave all-keys
                                                               (repeatedly (constantly val)))))]
         (fact "a valid report should should only contain true values"
               (validate-report {:team1 {:player1 1 :player2 2 :score 8}
                                 :team2 {:player1 3 :player2 4 :score 10}}) => (same-vals-fn true))
         (fact "an empty report should only contain nil values"
               (validate-report {}) => (same-vals-fn nil)))
       (fact "a report with invalid players should have validation for players added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => {:p1 false}
               (provided
                (#'foosball.validation.match/validate-players (#'foosball.validation.match/pick-players report)) => {:p1 false}
                (#'foosball.validation.match/validate-scores  (#'foosball.validation.match/pick-scores  report)) => {})))
       (fact "a report with invalid scores should have validation for scores added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => {:score false}
               (provided
                (#'foosball.validation.match/validate-players (#'foosball.validation.match/pick-players report)) => {}
                (#'foosball.validation.match/validate-scores  (#'foosball.validation.match/pick-scores  report)) => {:score false})))
       (fact "a report with invalid scores and invalid players should have validation for scores and players added"
             (let [report {:team1 ...team1... :team2 ...team2...}]
               (validate-report report) => {:players false :score false}
               (provided (#'foosball.validation.match/validate-players
                          (#'foosball.validation.match/pick-players report)) => {:players false}
                         (#'foosball.validation.match/validate-scores
                          (#'foosball.validation.match/pick-scores  report)) => {:score false}))))
