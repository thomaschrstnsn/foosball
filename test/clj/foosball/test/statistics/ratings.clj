(ns foosball.test.statistics.ratings
  (:require [foosball.statistics.ratings :as sut]
            [clojure.test :refer :all]
            [foosball.models.domains :as d]
            [foosball.test.helpers :as h]
            [schema.test :as schema.test]))

(use-fixtures :each h/only-error-log-fixture)
(use-fixtures :once schema.test/validate-schemas)

(deftest ratings
  (let [db         (h/memory-db)
        [p1 p2 p3 p4
         reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
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
                           :id (h/make-uuid)}))

        _          (doall (map (fn [m] (d/create-match! db m)) matches))
        get-db-players-fn (fn [players] (let [player-ids (set (map :id players))]
                                         (filter (fn [{:keys [id]}] (contains? player-ids id))
                                                 (d/get-players db))))
        players          #{p1 p2 p3 p4}
        players+reporter #{p1 p2 p3 p4 reporter}
        db-players       (get-db-players-fn players)
        db-players+reporter (get-db-players-fn players+reporter)
        db-matches (d/get-matches db)]
    (testing "calculate ratings"
      (let [ratings-result (sut/calculate-ratings  db-players db-matches)]
        (is (> 1601 (apply max (vals ratings-result))))
        (is (< 1398 (apply min (vals ratings-result))))
        (is (= (set (map :id db-players))
               (set (keys ratings-result))))
        (is (= 2 (-> ratings-result vals set count)))))
    (testing "calculate matchup"
      (is (not (= nil (sut/calculate-matchup db-matches db-players))))
      (is (not (= nil (sut/calculate-matchup db-matches db-players+reporter)))))))
