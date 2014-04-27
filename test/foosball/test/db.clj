(ns foosball.test.db
  (:require [foosball.models.db :refer :all]
            [com.stuartsierra.component :as c]
            [foosball.models.domains :as d]
            [foosball.util :as util]
            [clojure.test :refer :all]
            [clojure.data :as data]
            [taoensso.timbre :as t]
            [clojure.pprint :refer [pprint]]))

(defn memory-db []
  (c/start
   (map->Database {:uri (str "datomic:mem://foosball-" (java.util.UUID/randomUUID))})))

(defn log-fixture [f]
  (t/with-log-level :error
    (f)))

(use-fixtures :each log-fixture)

(defn diff-with-first-nil? [expected actual]
  (let [diff (first (data/diff expected actual))]
    (when diff (pprint {:diff diff}))
    (= nil (first (data/diff expected actual)))))

(defn make-uuid []
  (java.util.UUID/randomUUID))

(deftest database-tests
  (testing "Creating players:"
    (let [db (memory-db)]
      (testing "error on nil args"
        (is (thrown? java.util.concurrent.ExecutionException (d/create-player! db nil nil nil))))

      (testing "Upon creating a player:"
        (let [name   "thomas"
              openid "http://example.org/openid"
              id     (make-uuid)]
          (is (not= nil (d/create-player! db id name openid)))

          (testing "we can readout the player data"
            (is (= name (d/get-player db id)))
            (is (= #{openid} (d/get-player-openids db id))))

          (testing "we can see the player in the list of all players"
            (is (= [{:name name
                     :role :user
                     :active true
                     :id id}] (d/get-players db))))

          (testing "we can get the player be his openid"
            (is (diff-with-first-nil? {:playername name
                                       :playerid id} (d/get-player-with-given-openid db openid))))

          (testing "we can inactivate him,"
            (is (not= nil (d/deactivate-player! db id)))
            (testing "then he is inactive"
              (is (diff-with-first-nil? {:active false} (first (d/get-players db))))))

          (testing "we can reactivate him,"
            (is (not= nil (d/activate-player! db id)))
            (testing "then he is active again"
              (is (diff-with-first-nil? {:active true} (first (d/get-players db))))))
          (testing "we can rename our player,"
            (let [new-name "jeffrey"]
              (is (not= nil (d/rename-player! db id new-name)))
              (testing "then his name changed"
                (is (= new-name (d/get-player db id))))))))))

  (testing "Creating matches"
    (let [db            (memory-db)
          create-player (fn [seed]
                          (let [id (make-uuid)
                                name (str "name-" seed)
                                openid (str "openid-" seed)]
                            (d/create-player! db id name openid )
                            (util/symbols-as-map id name openid)))
          [p1 p2 p3 p4] (map create-player ["p1" "p2" "p3" "p3"])
          match-date (java.util.Date.)
          team1score 10
          team2score 5
          reporter (create-player "reporter")]
      (testing "We can create a match with our four players,"
        (is (not= nil (d/create-match! db {:matchdate match-date
                                           :team1 {:player1 (:id p1) :player2 (:id p2) :score team1score}
                                           :team2 {:player1 (:id p3) :player2 (:id p4) :score team2score}
                                           :reported-by (:id reporter)})))
        (testing "then we can get it out again"
          (let [result (d/get-matches db)]
            (is (diff-with-first-nil? {:matchdate match-date
                                       :team1 {:player1 (:name p1) :player2 (:name p2) :score team1score}
                                       :team2 {:player1 (:name p3) :player2 (:name p4) :score team2score}
                                       :reported-by (:name reporter)}
                                      (first result)))
            (is (= 1 (count result)))))))))
