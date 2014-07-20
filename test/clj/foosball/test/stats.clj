(ns foosball.test.stats
  (:require [clojure.test :refer :all]
            [conjure.core :refer [stubbing]]
            [foosball.statistics.core :refer :all]
            [foosball.statistics.ratings :refer :all]
            [foosball.statistics.team-player :refer :all]
            [foosball.util :refer [less-than-or-equal? greater-than-or-equal? less-than? greater-than?]]))

(deftest determine-winner-tests
  (let [winning-team     {:score 10 :player1 :...wp1... :player2 :...wp2...}
        losing-team      {:score  1 :player1 :...lp1... :player2 :...lp2...}
        expected-winners #{:...wp1... :...wp2...}
        expected-losers  #{:...lp1... :...lp2...}]
    (is (= {:team1 winning-team
            :team2 losing-team
            :something :...anything...
            :winners expected-winners
            :losers  expected-losers
            :players (set (concat expected-winners expected-losers))
            :score-delta [[expected-winners 9]
                          [expected-losers -9]]}
           (determine-winner {:team1 winning-team
                              :team2 losing-team
                              :something :...anything...})))))

(def example-matches
  [{:match/id 333, :matchdate #inst "2013-04-11",
    :team1 {:score 10, :player2 "Knud Erik", :player1 "Lisse", :id 5555},
    :team2 {:score 7, :player2 "Thomas", :player1 "Anders", :id 6666}}
   {:match/id 222, :matchdate #inst "2013-04-12",
    :team1 {:score 10, :player2 "Thomas", :player1 "Maria", :id 3333},
    :team2 {:score 2, :player2 "Lisse", :player1 "Anders", :id 4444}}
   {:match/id 111, :matchdate #inst "2013-04-13",
    :team1 {:score 10, :player2 "Lisse", :player1 "Thomas", :id 1111},
    :team2 {:score 8, :player2 "Maria", :player1 "Anders", :id 2222}}
   {:match/id 444, :matchdate #inst "2013-04-15",
    :team1 {:score 10, :player2 "Anders", :player1 "Knud Erik", :id 7777},
    :team2 {:score 6, :player2 "Lisse", :player1 "Maria", :id 8888}}])

(def all-players (set ["Anders" "Knud Erik" "Lisse" "Maria" "Thomas"]))

(deftest calculate-player-stats-tests
  (testing "the latest-matchdate"
    (is (= #{{:player "Thomas"    :latest-matchdate #inst "2013-04-13"}
             {:player "Anders"    :latest-matchdate #inst "2013-04-15"}
             {:player "Maria"     :latest-matchdate #inst "2013-04-15"}
             {:player "Knud Erik" :latest-matchdate #inst "2013-04-15"}
             {:player "Lisse"     :latest-matchdate #inst "2013-04-15"}}
           (set (map #(select-keys % [:player :latest-matchdate])
                     (calculate-player-stats example-matches)))))))

(deftest players-with-matches-by-date-tests
  (let [won-example-matches (map determine-winner example-matches)]
    (is (= #{}
           (players-with-matches-by-date less-than-or-equal? #inst "2013-01-01" won-example-matches)))
    (is (= all-players
           (players-with-matches-by-date greater-than-or-equal? #inst "2013-01-01" won-example-matches)))
    (is (= (disj all-players "Maria")
           (players-with-matches-by-date = #inst "2013-04-11" won-example-matches)))
    (is (= (disj all-players "Maria")
           (players-with-matches-by-date less-than? #inst "2013-04-12" won-example-matches)))
    (is (= all-players
           (players-with-matches-by-date less-than-or-equal? #inst "2013-04-12" won-example-matches)))
    (is (= #{}
           (players-with-matches-by-date greater-than? #inst "2013-04-15" won-example-matches)))))

(deftest calculate-player-stats-tests
  (is (= #{{:player "Thomas",   :wins 2, :losses 1, :total 3, :win-perc 200/3, :loss-perc 100/3 :score-delta  7}
           {:player "Lisse",    :wins 2, :losses 2, :total 4, :win-perc 50N,   :loss-perc 50N   :score-delta -7}
           {:player "Anders",   :wins 1, :losses 3, :total 4, :win-perc 25N,   :loss-perc 75N   :score-delta -9}
           {:player "Maria",    :wins 1, :losses 2, :total 3, :win-perc 100/3, :loss-perc 200/3 :score-delta 2}
           {:player "Knud Erik",:wins 2, :losses 0, :total 2, :win-perc 100,   :loss-perc 0     :score-delta 7}}
         (set (map #(select-keys % [:player :wins :losses :total :win-perc :loss-perc :score-delta])
                   (calculate-player-stats example-matches))))))

(deftest calculate-team-stats-tests
  (is (= #{{:team #{"Lisse" "Knud Erik"}, :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  3}
          {:team #{"Anders" "Maria"},    :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -2}
          {:team #{"Thomas" "Lisse"},    :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  2}
          {:team #{"Anders" "Knud Erik"} :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  4}
          {:team #{"Thomas" "Anders"},   :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -3}
          {:team #{"Lisse" "Anders"},    :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -8}
          {:team #{"Thomas" "Maria"},    :wins 1, :losses 0, :total 1, :win-perc 100, :loss-perc 0   :score-delta  8}
          {:team #{"Lisse" "Maria"},     :wins 0, :losses 1, :total 1, :win-perc 0,   :loss-perc 100 :score-delta -4}}
         (set (calculate-team-stats example-matches)))))

(def disabled-active-player-bonus {:inactive-ratings {} :active-player-bonus 0})

(deftest calculate-ratings-tests
  (testing "it should calculate as expected (without inactive-player-penalty)"
    (stubbing [inactive-players-rating disabled-active-player-bonus]
              (is (= {"Anders"    1468.883293697148,
                      "Knud Erik" 1532.883293697148,
                      "Lisse"     1498.5273111823453,
                      "Maria"     1483.7061014233586,
                      "Thomas"    1516.0}
                     (calculate-ratings all-players example-matches)))))
  (testing "it should work as a filter"
    (is (= {"Lisse" 1504.160551402851} (calculate-ratings ["Lisse"] example-matches)))
    (is (= {} (calculate-ratings [] example-matches))))
  (testing "it should work with a player without matches"
    (is (= {"non-existing" 1500} (calculate-ratings ["non-existing"] example-matches))))
  (testing "about ratings and a players continued inactivity"
    (let [first-match      {:team1 {:player1 "inactive" :player2 "inactive2" :score 7}
                            :team2 {:player1 "active2"  :player2 "active3"   :score 10}}
          active-team1     {:player1 "active1" :player2 "active2"}
          active-team2     {:player1 "active3" :player2 "active4"}
          team1-wins       {:team1 (merge active-team1 {:score 10})
                            :team2 (merge active-team2 {:score 7})}
          team2-wins       {:team1 (merge active-team1 {:score 7})
                            :team2 (merge active-team2 {:score 10})}
          inactive-matches (concat [first-match]
                                   (->> [team1-wins team2-wins] cycle (take 1000)))]
      (testing "the inactive player's rating can never become negative"
        (let [actual (calculate-ratings ["inactive"] inactive-matches)]
          (is (= 1 (count actual)))
          (is (pos? (actual "inactive"))))))))

(deftest calculate-reduced-log-for-player-tests
  (testing "it should match the example log for Thomas"
    (let [actual (calculate-reduced-log-for-player "Thomas" example-matches)]
      (is (= 4 (count actual)))
      (is (= [1484.0 1502.2946975602533 1520.0 1512.0]
             (map :new-rating actual)))))
  (let [inactive-two-matches (concat example-matches [(last example-matches)])]
    (testing "when being inactive for two matches in a row, only one inactivity log as created"
      (let [actual (calculate-reduced-log-for-player "Thomas" inactive-two-matches)]
        (is (= 4 (count actual)))
        (is (= [1484.0 1502.2946975602533 1520.0 1504.0]
               (map :new-rating actual)))))
    (testing "inactivity log contains expected data"
      (is (= {:log-type   :inactivity
              :delta      -16.0
              :inactivity 2}
             (select-keys (last (calculate-reduced-log-for-player "Thomas" inactive-two-matches))
                          [:log-type :delta :inactivity]))))))

(deftest calculate-form-helper-tests
  (let [{:keys [won-matches]} (ratings-with-log [] example-matches)
        calculate-form-helper (fn [num-matches player]
                                (get (calculate-form-from-matches won-matches num-matches) player))]
    (are [n player expected]
      (= expected (calculate-form-helper n player))
      5 "Thomas"    [false true true]
      2 "Thomas"    [true true]
      1 "Thomas"    [true]
      5 "Anders"    [false false false true]
      5 "Lisse"     [true false true false]
      5 "Knud Erik" [true true]
      5 "Maria"     [true false false])))

(deftest possible-matchups-tests
  (testing "the number of results is bound to the size of the input"
    (are [input expected-count]
      (= expected-count (count (possible-matchups (range input))))
      4 3
      5 15
      6 45
      7 105)))
