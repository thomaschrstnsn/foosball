(ns foosball.test.statistics.core
  (:require [foosball.statistics.core :as sut]
            [clojure.test :refer :all]
            [foosball.models.domains :as d]
            [foosball.test.helpers :as h]
            [schema.test :as schema.test]))

(use-fixtures :each h/only-error-log-fixture)
(use-fixtures :once schema.test/validate-schemas)

(deftest core
  (let [db         (h/memory-db)
        [p1 p2 p3 p4
         reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
        league     (h/create-dummy-league db "leaguex")
        matches    (vec (for [index (range 8)
                              :let [team1score 10
                                    team2score index
                                    match-date (java.util.Date.)
                                    t1p1 p1
                                    t1p2 p2
                                    t2p1 p3
                                    t2p2 p4]]
                          {:matchdate match-date
                           :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                           :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                           :reported-by (:id reporter)
                           :id (h/make-uuid)
                           :league-id (:id league)}))

        _          (doall (map (fn [m] (d/create-match! db m)) matches))
        player-ids (set (mapv :id [p1 p2 p3 p4]))
        db-players (filter (fn [{:keys [id]}] (contains? player-ids id))
                           (d/get-players db))
        db-matches (d/get-matches db)
        id-set-from-players (fn [& players] (->> players (map :id) set))]
    (testing "determine-winner"
      (let [result (sut/determine-winner (first db-matches))
            winner-ids (id-set-from-players p1 p2)
            loser-ids  (id-set-from-players p3 p4)]
        (is (= winner-ids (:winners result)))
        (is (= loser-ids  (:losers result)))
        (is (= (id-set-from-players p1 p2 p3 p4) (:players result)))
        (is (= [[winner-ids 10]
                [loser-ids -10]] (:score-delta result)))))
    (testing "players-from-matches"
      (let [matches (map sut/determine-winner db-matches)]
        (is (= (id-set-from-players p1 p2 p3 p4) (sut/players-from-matches matches)))))
    (testing "teams-from-matches"
      (let [matches (map sut/determine-winner db-matches)]
        (is (= #{(id-set-from-players p1 p2)
                 (id-set-from-players p3 p4)} (sut/teams-from-matches matches)))))
    (testing "winners and losers: "
      (let [match (sut/determine-winner (first db-matches))]
        (are [player expected-winner?]
          (= expected-winner? (sut/player-is-winner? (:id player) match))
          p1 true
          p2 true
          p3 false
          p4 false
          reporter false)
        (are [player expected-loser?]
          (= expected-loser? (sut/player-is-loser? (:id player) match))
          p1 false
          p2 false
          p3 true
          p4 true
          reporter false)))))
