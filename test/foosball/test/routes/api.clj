(ns foosball.test.routes.api
  (:require [foosball.models.domains :as d]
            [foosball.test.helpers :as h]
            [foosball.test.response-helpers :as rh]
            [clojure.test :refer :all]
            [ring.mock.request :as mockr]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [liberator.dev :as libdev]))

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
              10 4)))))))
