(ns foosball.test.db
  (:require [foosball.models.db :refer :all]
            [com.stuartsierra.component :as c]
            [foosball.models.domains :as d]
            [clojure.test :refer :all]
            [taoensso.timbre :as t]))

(defn memory-db []
  (c/start
   (map->Database {:uri (str "datomic:mem://foosball-" (java.util.UUID/randomUUID))})))

(defn log-fixture [f]
  (t/with-log-level :error
    (f)))

(use-fixtures :each log-fixture)

(defn intersect-keys= [expected actual]
  (let [keys-to-compare (keys expected)]
    (= expected (select-keys actual keys-to-compare))))

(deftest database-tests
  (testing "Creating players:"
    (let [db (memory-db)
          uuid-fn (fn [] (java.util.UUID/randomUUID))]
      (testing "error on nil args"
        (is (thrown? java.util.concurrent.ExecutionException (d/create-player! db nil nil nil))))

      (testing "Upon creating a player:"
        (let [name   "thomas"
              openid "http://example.org/openid"
              id   (uuid-fn)]
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
            (is (intersect-keys= {:playername name
                                  :playerid id} (d/get-player-with-given-openid db openid))))

          (testing "we can inactivate him,"
            (is (not= nil (d/deactivate-player! db id)))
            (testing "then he is inactive"
              (is (intersect-keys= {:active false} (first (d/get-players db))))))

          (testing "we can reactivate him,"
            (is (not= nil (d/activate-player! db id)))
            (testing "then he is active again"
              (is (intersect-keys= {:active true} (first (d/get-players db))))))
          (testing "we can rename our player,"
            (let [new-name "jeffrey"]
              (is (not= nil (d/rename-player! db id new-name)))
              (testing "then his name changed"
                (is (= new-name (d/get-player db id)))))))))))
