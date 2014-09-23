(ns foosball.test.stats
  (:require [clojure.test :refer :all]
            [conjure.core :refer [stubbing]]
            [foosball.statistics.core :refer :all]
            [foosball.statistics.ratings :refer :all]
            [foosball.statistics.team-player :refer :all]
            [foosball.util :refer [less-than-or-equal? greater-than-or-equal? less-than? greater-than?] :as util]))

(defn- create-player [name]
  {:name name :id (util/create-uuid)})

(def players {:anders    (create-player "Anders")
              :knud-erik (create-player "Knud Erik")
              :lisse     (create-player "Lisse")
              :maria     (create-player "Maria")
              :thomas    (create-player "Thomas")})

(def all-players (set (vals players)))

(def example-matches
  [{:match/id 333, :matchdate #inst "2013-04-11"
    :team1 {:score 10 :player2 (:knud-erik players) :player1 (:lisse players)  :id 5555}
    :team2 {:score 7  :player2 (:thomas players)    :player1 (:anders players) :id 6666}}
   {:match/id 222, :matchdate #inst "2013-04-12"
    :team1 {:score 10 :player2 (:thomas players) :player1 (:maria players)  :id 3333}
    :team2 {:score 2  :player2 (:lisse players)  :player1 (:anders players) :id 4444}}
   {:match/id 111, :matchdate #inst "2013-04-13"
    :team1 {:score 10 :player2 (:lisse players) :player1 (:thomas players) :id 1111}
    :team2 {:score 8  :player2 (:maria players) :player1 (:anders players) :id 2222}}
   {:match/id 444, :matchdate #inst "2013-04-15"
    :team1 {:score 10 :player2 (:anders players) :player1 (:knud-erik players) :id 7777}
    :team2 {:score 6  :player2 (:lisse players)  :player1 (:maria players)     :id 8888}}])

(defn- player-id [kw]
  (-> players kw :id))

(deftest calculate-player-stats-tests
  (testing "the latest-matchdate"
    (is (= #{{:player (player-id :thomas)    :latest-matchdate #inst "2013-04-13"}
             {:player (player-id :anders)    :latest-matchdate #inst "2013-04-15"}
             {:player (player-id :maria)     :latest-matchdate #inst "2013-04-15"}
             {:player (player-id :knud-erik) :latest-matchdate #inst "2013-04-15"}
             {:player (player-id :lisse)     :latest-matchdate #inst "2013-04-15"}}
           (set (map #(select-keys % [:player :latest-matchdate])
                     (calculate-player-stats example-matches)))))))

(def every-id (set (map :id all-players)))

(deftest players-with-matches-by-date-tests
  (let [won-example-matches (map determine-winner example-matches)]
    (is (= #{}
           (players-with-matches-by-date less-than-or-equal? #inst "2013-01-01" won-example-matches)))
    (is (= every-id
           (players-with-matches-by-date greater-than-or-equal? #inst "2013-01-01" won-example-matches)))
    (is (= (disj every-id (player-id :maria))
           (players-with-matches-by-date = #inst "2013-04-11" won-example-matches)))
    (is (= (disj every-id (player-id :maria))
           (players-with-matches-by-date less-than? #inst "2013-04-12" won-example-matches)))
    (is (= every-id
           (players-with-matches-by-date less-than-or-equal? #inst "2013-04-12" won-example-matches)))
    (is (= #{}
           (players-with-matches-by-date greater-than? #inst "2013-04-15" won-example-matches)))))

(deftest calculate-player-stats-tests
  (is
   (=
    #{{:player (player-id :thomas)    :wins 2 :losses 1 :total 3 :win-perc 200/3 :loss-perc 100/3 :score-delta  7}
      {:player (player-id :lisse)     :wins 2 :losses 2 :total 4 :win-perc 50N   :loss-perc 50N   :score-delta -7}
      {:player (player-id :anders)    :wins 1 :losses 3 :total 4 :win-perc 25N   :loss-perc 75N   :score-delta -9}
      {:player (player-id :maria)     :wins 1 :losses 2 :total 3 :win-perc 100/3 :loss-perc 200/3 :score-delta 2}
      {:player (player-id :knud-erik) :wins 2 :losses 0 :total 2 :win-perc 100   :loss-perc 0     :score-delta 7}}
    (set (map #(select-keys % [:player :wins :losses :total :win-perc :loss-perc :score-delta])
              (calculate-player-stats example-matches))))))

(deftest calculate-team-stats-tests
  (let [team-ids-fn      (fn [p1kw p2kw] (->> [p1kw p2kw] (map player-id) set))
        lisse+knud-erik  (team-ids-fn :lisse :knud-erik)
        anders+maria     (team-ids-fn :anders :maria)
        thomas+lisse     (team-ids-fn :thomas :lisse)
        anders+knud-erik (team-ids-fn :anders :knud-erik)
        thomas+anders    (team-ids-fn :thomas :anders)
        lisse+anders     (team-ids-fn :lisse :anders)
        thomas+maria     (team-ids-fn :thomas :maria)
        lisse+maria      (team-ids-fn :lisse :maria)]
    (is (= #{{:team lisse+knud-erik  :wins 1 :losses 0 :total 1 :win-perc 100 :loss-perc 0   :score-delta  3}
             {:team anders+maria     :wins 0 :losses 1 :total 1 :win-perc 0   :loss-perc 100 :score-delta -2}
             {:team thomas+lisse     :wins 1 :losses 0 :total 1 :win-perc 100 :loss-perc 0   :score-delta  2}
             {:team anders+knud-erik :wins 1 :losses 0 :total 1 :win-perc 100 :loss-perc 0   :score-delta  4}
             {:team thomas+anders    :wins 0 :losses 1 :total 1 :win-perc 0   :loss-perc 100 :score-delta -3}
             {:team lisse+anders     :wins 0 :losses 1 :total 1 :win-perc 0   :loss-perc 100 :score-delta -8}
             {:team thomas+maria     :wins 1 :losses 0 :total 1 :win-perc 100 :loss-perc 0   :score-delta  8}
             {:team lisse+maria      :wins 0 :losses 1 :total 1 :win-perc 0   :loss-perc 100 :score-delta -4}}
           (set (calculate-team-stats example-matches))))))

(def disabled-active-player-bonus {:inactive-ratings {} :active-player-bonus 0})

(deftest calculate-ratings-tests
  (testing "it should calculate as expected (without inactive-player-penalty)"
    (stubbing [inactive-players-rating disabled-active-player-bonus]
              (is (= {(player-id :anders)    1468.883293697148,
                      (player-id :knud-erik) 1532.883293697148,
                      (player-id :lisse)     1498.5273111823453,
                      (player-id :maria)     1483.7061014233586,
                      (player-id :thomas)    1516.0}
                     (calculate-ratings all-players example-matches)))))
  (testing "it should work as a filter"
    (is (= {(player-id :lisse) 1504.160551402851} (calculate-ratings [(:lisse players)] example-matches)))
    (is (= {} (calculate-ratings [] example-matches))))
  (testing "it should work with a player without matches"
    (let [non-existing (create-player "non-existing")]
      (is (= {(:id non-existing) 1500} (calculate-ratings [non-existing] example-matches)))))
  (testing "about ratings and a players continued inactivity"
    (let [create-player-sequence-fn (fn [seed num]
                                      (mapv (fn [n] (create-player (str seed n))) (range 1 (inc num))))
          [active1 active2
           active3 active4]         (create-player-sequence-fn "active" 4)
          [inactive inactive2]     (create-player-sequence-fn "inactive" 2)
          first-match               {:team1 {:player1 inactive :player2 inactive2 :score 7}
                                     :team2 {:player1 active2  :player2 active3   :score 10}}
          active-team1              {:player1 active1 :player2 active2}
          active-team2              {:player1 active3 :player2 active4}
          team1-wins                {:team1 (merge active-team1 {:score 10})
                                     :team2 (merge active-team2 {:score 7})}
          team2-wins                {:team1 (merge active-team1 {:score 7})
                                     :team2 (merge active-team2 {:score 10})}
          inactive-matches          (concat [first-match]
                                            (->> [team1-wins team2-wins] cycle (take 1000)))]
      (testing "the inactive player's rating can never become negative"
        (let [actual (calculate-ratings [inactive] inactive-matches)]
          (is (= 1 (count actual)))
          (is (pos? (actual (:id inactive)))))))))

(deftest calculate-reduced-log-for-player-tests
  (testing "it should match the example log for Thomas"
    (let [actual (calculate-reduced-log-for-player (:thomas players) example-matches)]
      (is (= 4 (count actual)))
      (is (= [1484.0 1502.2946975602533 1520.0 1512.0]
             (map :new-rating actual)))))
  (let [inactive-two-matches (concat example-matches [(last example-matches)])]
    (testing "when being inactive for two matches in a row, only one inactivity log as created"
      (let [actual (calculate-reduced-log-for-player (:thomas players) inactive-two-matches)]
        (is (= 4 (count actual)))
        (is (= [1484.0 1502.2946975602533 1520.0 1504.0]
               (map :new-rating actual)))))
    (testing "inactivity log contains expected data"
      (is (= {:log-type   :inactivity
              :delta      -16.0
              :inactivity 2}
             (select-keys (last (calculate-reduced-log-for-player (:thomas players) inactive-two-matches))
                          [:log-type :delta :inactivity]))))))

(deftest calculate-form-helper-tests
  (let [{:keys [won-matches]} (ratings-with-log [] example-matches)
        calculate-form-helper (fn [num-matches player]
                                (get (calculate-form-from-matches won-matches num-matches) player))]
    (are [n player expected]
      (= expected (calculate-form-helper n player))
      5 (player-id :thomas)    [false true true]
      2 (player-id :thomas)    [true true]
      1 (player-id :thomas)    [true]
      5 (player-id :anders)    [false false false true]
      5 (player-id :lisse)     [true false true false]
      5 (player-id :knud-erik) [true true]
      5 (player-id :maria)     [true false false])))

(deftest possible-matchups-tests
  (testing "the number of results is bound to the size of the input"
    (are [input expected-count]
      (= expected-count (count (possible-matchups (range input))))
      4 3
      5 15
      6 45
      7 105)))
