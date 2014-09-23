(ns foosball.test.db
  (:require [foosball.models.domains :as d]
            [foosball.util :as util]
            [foosball.test.helpers :as h]
            [clojure.test :refer :all]
            [schema.test :as schema.test]))

(use-fixtures :each h/only-error-log-fixture)
(use-fixtures :once schema.test/validate-schemas)

(deftest database-tests
  (testing "Creating players:"
    (let [db (h/memory-db)]
      (testing "Upon creating a player:"
        (let [name   "thomas"
              openid "http://example.org/openid"
              id     (h/make-uuid)]
          (is (not= nil (d/create-player! db id name openid)))

          (testing "we can readout the player data"
            (is (= name (:name (d/get-player db id))))
            (is (= #{openid} (d/get-player-openids db id))))

          (testing "we can see the player in the list of all players"
            (is (= [{:name name
                     :role :user
                     :active true
                     :id id}] (d/get-players db))))

          (testing "we can get the player be his openid"
            (is (h/diff-with-first-is-nil? {:playername name
                                            :playerid id} (d/get-player-with-given-openid db openid))))

          (testing "we can inactivate him,"
            (is (not= nil (d/deactivate-player! db id)))
            (testing "then he is inactive"
              (is (h/diff-with-first-is-nil? {:active false} (first (d/get-players db))))))

          (testing "we can reactivate him,"
            (is (not= nil (d/activate-player! db id)))
            (testing "then he is active again"
              (is (h/diff-with-first-is-nil? {:active true} (first (d/get-players db))))))
          (testing "we can rename our player,"
            (let [new-name "jeffrey"]
              (is (not= nil (d/rename-player! db id new-name)))
              (testing "then his name changed"
                (is (= new-name (:name (d/get-player db id)))))))))))

  (testing "Creating matches"
    (let [db              (h/memory-db)
          [p1 p2 p3 p4]   (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p3"])
          player-from-exp (fn [e] (select-keys e [:id :name]))
          match-date      (java.util.Date.)
          team1score      10
          team2score      5
          reporter        (h/create-dummy-player db "reporter")
          match-id        (h/make-uuid)
          create-match    (fn [& {:keys [id]}]
                            (d/create-match! db {:matchdate match-date
                                                 :id id
                                                 :team1 {:player1 (:id p1) :player2 (:id p2) :score team1score}
                                                 :team2 {:player1 (:id p3) :player2 (:id p4) :score team2score}
                                                 :reported-by (:id reporter)}))]
      (testing "We can create a match with our four players,"
        (is (not= nil (create-match :id match-id)))
        (let [expected-match {:matchdate match-date
                              :team1 {:player1 (player-from-exp p1)
                                      :player2 (player-from-exp p2)
                                      :score team1score}
                              :team2 {:player1 (player-from-exp p3)
                                      :player2 (player-from-exp p4 )
                                      :score team2score}
                              :reported-by (player-from-exp reporter)}]
          (testing "then we can get it out again using get-matches"
            (let [result (d/get-matches db)]
              (is (h/diff-with-first-is-nil? expected-match
                                             (first result)))
              (is (= 1 (count result)))))

          (testing "then we can get it out again using get-by-id"
            (let [result (d/get-match db match-id)]
              (is (h/diff-with-first-is-nil? expected-match result)))))

        (testing "; and we can delete it again"
          (is (not= nil match-id))
          (is (not= nil (d/delete-match! db match-id)))
          (testing ", and we cannot get it out again."
            (is (empty? (d/get-matches db)))
            (is (nil? (d/get-match db match-id)))))

        (testing "Creation of multiple matches"
          (let [num-matches 100
                created-matches (doall (repeatedly num-matches create-match))]
            (is (= num-matches (count (d/get-matches db))))))))))
