(ns foosball.test.routes.api
  (:require [foosball.models.domains :as d]
            [foosball.test.helpers :as h]
            [foosball.test.response-helpers :as rh]
            [clojure.test :refer :all]
            [ring.mock.request :as mockr]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [liberator.dev :as libdev]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]))

(use-fixtures :each h/only-error-log-fixture)

(defn test-route-for-supported-media-types [handler request]
  (testing "Accept header decides result"
            (are [mime-type]
              (= 200 (-> request
                         (mockr/header :accept mime-type)
                         handler
                         :status))
              "application/edn"
              "text/html"
              "application/json")))

(deftest api-route-tests
  (testing "GET '/api/players':"
    (let [app     (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/players")]
      (testing "without any players in the database"
        (is (= '() (-> request handler :body edn/read-string)))
        (is (some #{"application/edn" "charset=UTF-8"} (-> request handler rh/content-type))))
      (testing "with player in the database;"
        (let [db     (:database app)
              name   "thomas"
              openid "http://example.org/openid"
              id     (h/make-uuid)
              _      (d/create-player! db id name openid)]
          (is (= {:role :user
                  :active true
                  :name name
                  :id id} (-> request handler :body edn/read-string first)))
          (test-route-for-supported-media-types handler request)
          (testing "JSON output is parsable into something similar"
            (is  (= {:role "user"
                     :name name
                     :active true
                     :id (str id)} (-> request
                                       (mockr/header :accept "application/json")
                                       handler
                                       :body
                                       (json/read-str :key-fn keyword)
                                       first))))))))
  (testing "GET '/api/ratings/leaderboard':"
    (let [app     (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/ratings/leaderboard/5")]
      (testing "without any players in the database"
        (is (some #{"application/edn" "charset=UTF-8"} (-> request handler rh/content-type)))
        (is (empty? (-> request handler :body edn/read-string))))

      (test-route-for-supported-media-types handler request)

      (testing "with players and matches in the database"
        (let [db (:database app)
              [p1 p2 p3 p4 reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
              match-date (java.util.Date.)
              team1score 10
              team2score 7
              create-match (fn [t1p1 t1p2 t2p1 t2p2]
                             (d/create-match! db {:matchdate match-date
                                                  :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                                                  :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                                                  :reported-by (:id reporter)}))
              ;;;; should give: p1 > p2 > p3 > p4
              _ (doall (map (fn [i] (create-match p1 p2 p3 p4)) (range 15)))
              _ (create-match p1 p3 p2 p4)
              response (-> request handler :body edn/read-string)]
          (is (= 4 (count response)))
          (is (= (map :name [p1 p2 p3 p4]) (->> response (map :player/name))))
          (is (apply > (map :rating response)))
          (is (= [1 2 3 4] (vec (map :position response))))

          (testing "; with different request params"
            (are [request-size expected-size]
              (= expected-size (-> (mockr/request :get (str "/api/ratings/leaderboard/" request-size))
                                   handler
                                   :body
                                   edn/read-string
                                   count))
              1 1
              2 2
              3 3
              4 4
              5 4
              10 4))))))

  (testing "GET '/api/ratings/log/{uuid}':"
    (let [app     (h/app-with-memory-db)
          handler (:ring-handler app)
          make-request (fn [id] (mockr/request :get (str "/api/ratings/log/" id)))]
      (testing "with an empty database, any uuid returns empty"
        (is (= '() (-> (make-request (h/make-uuid)) handler :body edn/read-string))))

      (testing "with players and matches in the database,"
        (let [db (:database app)
              [p1 p2 p3 p4 p5 p6 reporter] (map (partial h/create-dummy-player db)
                                                ["p1" "p2" "p3" "p4" "p5" "p6" "rep"])
              today (time/local-date 2014 4 25)
              yesterday (time/local-date 2014 4 24)
              day-before-last (time/local-date 2014 4 23)

              team1score 10
              team2score 7
              number-of-matches 15
              create-match (fn [match-date t1p1 t1p2 t2p1 t2p2]
                             (d/create-match! db {:matchdate (time-coerce/to-date match-date)
                                                  :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                                                  :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                                                  :reported-by (:id reporter)}))
              _ (create-match day-before-last p1 p2 p3 p6)
              _ (doall (map (fn [i] (create-match yesterday p1 p2 p3 p4)) (range number-of-matches)))
              _ (create-match today p2 p3 p4 p5)

              response-for-player (fn [{:keys [id]}] (-> (make-request id) handler :body edn/read-string))
              filter-inactivity (fn [{:keys [log-type]}] (= log-type :inactivity))
              filter-active     (complement filter-inactivity)]
          (testing "player 'p1':"
            (testing "plays 16 matches"
              (is (= 16 (->> (response-for-player p1)
                             (filter filter-active)
                             (count)))))
            (testing "has one inactivity"
              (is (= 1 (->> (response-for-player p1)
                            (filter filter-inactivity)
                            (count))))
              (is (= 1 (->> (response-for-player p1)
                            (filter filter-inactivity)
                            first
                            :inactivity)))))
          (testing "player 'p5':"
            (testing "plays for one match"
              (is (= 1 (->> (response-for-player p5)
                            (filter filter-active)
                            (count)))))
            (testing "is not inactive"
              (is (= 0 (->> (response-for-player p5)
                            (filter filter-inactivity)
                            (count))))))
          (testing "player 'p6':"
            (testing "has 1 active"
              (is (= 1 (->> (response-for-player p6)
                            (filter filter-active)
                            (count)))))
            (testing "has 16 inactive"
              (is (= 16 (->> (response-for-player p6)
                             (filter filter-inactivity)
                             first
                             :inactivity))))))))))
