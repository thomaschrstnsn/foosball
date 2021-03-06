(ns foosball.test.routes.api
  (:require [foosball.models.domains :as d]
            [foosball.auth :refer [current-auth] :as auth]
            [foosball.test.helpers :as h]
            [foosball.test.response-helpers :as rh]
            [foosball.util :as util]
            [clojure.test :refer :all]
            [ring.mock.request :as mockr]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [conjure.core :refer [stubbing]]
            [datomic.api :as datomic]
            [liberator.dev :as libdev]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [schema.test :as schema.test]))

(use-fixtures :each h/only-error-log-fixture)
(use-fixtures :once schema.test/validate-schemas)

(defn test-route-for-supported-media-types [handler request]
  (testing "Accept header decides result:"
    (let [valid-mime-request? (fn [mime-type]
                                (= 200 (-> request
                                           (mockr/header :accept mime-type)
                                           handler
                                           :status)))]
      (is (valid-mime-request? "application/edn"))
      (is (valid-mime-request? "text/html"))
      (is (valid-mime-request? "application/json")))))

(defn transact-something-in-db! [db]
  (let [eid  (datomic/tempid :db.part/user)
        uuid (h/make-uuid)
        conn (:connection db)]
    @(datomic/transact conn [{:db/id eid :player/id uuid}])))

(defn test-route-for-etag-with-database-transaction [handler request db]
  (let [response (handler request)
        etag     (get-in response [:headers "ETag"] :not-found)]
    (testing "Reponse contains etag header:"
      (is (= false (contains? #{nil "" :not-found} etag))))
    (testing "Request with if-none-match correct returns 304 (Not Modified):"
      (let [cond-request  (mockr/header request "if-none-match" etag)
            cond-response (-> cond-request handler)]
        (is (= 304 (:status cond-response)))))
    (testing "Request with if-none-match, after database transaction, returns 200:"
      (let [_ (transact-something-in-db! db)
            cond-request  (mockr/header request "if-none-match" etag)
            cond-response (-> cond-request handler)]
        (is (= 200 (:status cond-response)))))))

(defn test-route-to-not-include-etag [handler request]
  (let [response (handler request)
        etag     (get-in response [:headers "ETag"] :not-found)]
    (testing "Reponse must not contain etag header:"
      (is (= :not-found etag)))))

(defn player-from-exp [e] (select-keys e [:id :name]))

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
          (test-route-for-etag-with-database-transaction handler request db)
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
      (test-route-for-etag-with-database-transaction handler request (:database app))

      (testing "with players and matches in the database"
        (let [db (:database app)
              [p1 p2 p3 p4 reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
              match-date (java.util.Date.)
              league (h/create-dummy-league db "someleague")
              team1score 10
              team2score 7
              create-match (fn [t1p1 t1p2 t2p1 t2p2]
                             (d/create-match! db {:matchdate match-date
                                                  :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                                                  :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                                                  :reported-by (:id reporter)
                                                  :league-id (:id league)}))
              ;; should give: p1 > p2 > p3 > p4
              _ (doall (map (fn [i] (create-match p1 p2 p3 p4)) (range 15)))
              _ (create-match p1 p3 p2 p4)
              response (-> request handler :body edn/read-string)]
          (is (= 4 (count response)))
          (is (= (map :id [p1 p2 p3 p4]) (->> response (map :player/id))))
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

      (test-route-for-etag-with-database-transaction handler
                                                     (make-request (h/make-uuid))
                                                     (:database app))

      (testing "with players and matches in the database,"
        (let [db (:database app)
              [p1 p2 p3 p4 p5 p6 reporter] (map (partial h/create-dummy-player db)
                                                ["p1" "p2" "p3" "p4" "p5" "p6" "rep"])
              today (time/local-date 2014 4 25)
              yesterday (time/local-date 2014 4 24)
              day-before-last (time/local-date 2014 4 23)

              league (h/create-dummy-league db "aleague")

              team1score 10
              team2score 7
              number-of-matches 15
              create-match (fn [match-date t1p1 t1p2 t2p1 t2p2]
                             (d/create-match! db {:matchdate (time-coerce/to-date match-date)
                                                  :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                                                  :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                                                  :reported-by (:id reporter)
                                                  :league-id (:id league)}))
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
                             :inactivity)))))))))

  (testing "GET '/api/matches':"
    (let [app     (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/matches")]
      (testing "without any players in the database"
        (is (some #{"application/edn" "charset=UTF-8"} (-> request handler rh/content-type)))
        (is (empty? (-> request handler :body edn/read-string))))

      (test-route-for-supported-media-types handler request)
      (test-route-for-etag-with-database-transaction handler request (:database app))

      (testing "with players and matches in the database"
        (let [db (:database app)
              [p1 p2 p3 p4 reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
              league  (h/create-dummy-league db "league")
              matches (vec (for [index (range 8)
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

              _ (doall (map (fn [m] (d/create-match! db m)) matches))
              response (-> request handler :body edn/read-string)]
          (testing "first teams match expected"
            (let [expected-team1s (vec (map (fn [{:keys [team1]}] {:score (:score team1)
                                                                  :player1 (player-from-exp p1)
                                                                  :player2 (player-from-exp p2)})
                                            matches))]
              (is (h/seq-diff-with-first-is-nil? expected-team1s (map :team1 response)))))
          (testing "second teams match expected"
            (let [expected-team2s (vec (map (fn [{:keys [team2]}] {:score   (:score team2)
                                                                  :player1 (player-from-exp p3)
                                                                  :player2 (player-from-exp p4)})
                                            matches))]
              (is (h/seq-diff-with-first-is-nil? expected-team2s (map :team2 response)))))
          (testing "match data match expected"
            (let [expected-matches (vec (map  (fn [{:keys [id matchdate]}]
                                                {:match/id id
                                                 :reported-by (player-from-exp reporter)
                                                 :matchdate matchdate})
                                              matches))]
              (is (h/seq-diff-with-first-is-nil? expected-matches
                                                 (map (fn [m] (select-keys m [:match/id
                                                                             :reported-by
                                                                             :matchdate])) response)))))))))

  (testing "RESOURCE '/api/match/{guid}':"
    (let [app     (h/app-with-memory-db)
          handler (:ring-handler app)
          build-request (fn [op id] (mockr/request op (str "/api/match/" id)))]
      (testing "with players and matches in the database"
        (let [db (:database app)
              [p1 p2 p3 p4 reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
              league (h/create-dummy-league db "someleague")
              new-match-fn (fn []
                             (let [team1score 10
                                   team2score 4
                                   match-date (java.util.Date.)
                                   t1p1 p1
                                   t1p2 p2
                                   t2p1 p3
                                   t2p2 p4]
                               {:matchdate match-date
                                :team1 {:player1 (:id t1p1) :player2 (:id t1p2) :score team1score}
                                :team2 {:player1 (:id t2p1) :player2 (:id t2p2) :score team2score}
                                :reported-by (:id reporter)
                                :id (h/make-uuid)
                                :league-id (:id league)}))]
          (testing "without valid auth GET/DELETE is 401"
            (let [expected-match (new-match-fn)
                  _              (d/create-match! db expected-match)
                  match-id       (:id expected-match)]
              (are [method]
                (= 401 (:status (-> (build-request method match-id) handler)))
                :get :delete)))
          (testing "with valid auth, without playerid allowed GET/DELETE is 403"
            (stubbing [current-auth {:bogus-auth :yeah}]
                      (let [expected-match (new-match-fn)
                            _              (d/create-match! db expected-match)
                            match-id       (:id expected-match)]
                        (are [method]
                          (= 403 (:status (-> (build-request method match-id) handler)))
                          :get :delete))))
          (testing "with valid auth, with playerid without admin: GET is 200, DELETE is 403"
            (stubbing [current-auth {:playerid :yeah}]
                      (let [expected-match (new-match-fn)
                            _              (d/create-match! db expected-match)
                            match-id       (:id expected-match)]
                        (are [method status]
                          (= status (:status (-> (build-request method match-id) handler)))
                          :get    200
                          :delete 403))))
          (testing "with valid auth, with playerid and admin: GET is 200, DELETE is 204"
            (stubbing [current-auth {:playerid :yeah
                                     :roles    [auth/admin]}]
                      (let [expected-match (new-match-fn)
                            _              (d/create-match! db expected-match)
                            match-id       (:id expected-match)]
                        (are [method status]
                          (= status (:status (-> (build-request method match-id) handler)))
                          :get    200
                          :delete 204))))
          (stubbing
           [current-auth {:logged-in? true
                          :playerid   (:id reporter)
                          :roles      [auth/admin]}]
           (testing "with valid-auth"
             (testing "getting db-created match"
               (let [expected-match (new-match-fn)
                     _              (d/create-match! db expected-match)
                     match-id       (:id expected-match)
                     request        (build-request :get match-id)
                     response       (-> request handler)
                     response-body  (-> response :body edn/read-string)]
                 (test-route-for-etag-with-database-transaction handler
                                                                request
                                                                (:database app))
                 (is (= 200 (:status response)))
                 (let [{:keys [team1 team2 matchdate]} response-body]
                   (is (= true (every? identity [team1 team2 matchdate]))))
                 (test-route-for-supported-media-types handler request)
                 (let [expected-team-fn (fn [expected-match team-key [p1 p2]]
                                          (let [team (team-key expected-match)]
                                            {:score (:score team)
                                             :player1 (player-from-exp p1)
                                             :player2 (player-from-exp p2)}))
                       actual-team-fn (fn [response team-key]
                                        (select-keys (team-key response) [:score :player1 :player2]))]
                   (testing "first team match expected"
                     (is (= (expected-team-fn expected-match :team1 [p1 p2])
                            (actual-team-fn response-body :team1))))
                   (testing "second team match expected"
                     (is (= (expected-team-fn expected-match :team2 [p3 p4])
                            (actual-team-fn response-body :team2))))
                   (testing "match data match expected"
                     (let [expected-match-fn  (fn [{:keys [id matchdate]}]
                                                {:match/id match-id
                                                 :reported-by (player-from-exp reporter)
                                                 :matchdate matchdate})
                           actual-match-fn   (fn [response]
                                               (select-keys response
                                                            [:match/id :reported-by :matchdate]))]
                       (is (= (expected-match-fn expected-match)
                              (actual-match-fn response-body))))))))
             (let [build-post-req (fn [match]
                                    (-> (build-request :post (:id match))
                                        (mockr/header :content-type "application/edn")
                                        (update-in [:body]
                                                   (fn [_] (prn-str match)))))]
               (testing "POST with incomplete request gives status 400"
                 (let [post-request-fn      (fn [mangle-fn]
                                              (let [mangled-match (-> (new-match-fn) mangle-fn)
                                                    match-id (:id mangled-match)]
                                                (build-post-req mangled-match)))
                       mangle               (fn [filtered-keys m]
                                              (util/filter-keys m filtered-keys))
                       handler              (-> handler (liberator.dev/wrap-trace :header))
                       post-mangled-request (fn [filtered-keys]
                                              (-> (post-request-fn (partial mangle filtered-keys))
                                                  handler))]
                   (are [excluded-key] (= 400 (-> (post-mangled-request [excluded-key]) :status))
                        :league-id :matchdate :team1 :team2 :reported-by)))
               (testing "POST/GET/DELETE/GET a match with edn: "
                 (let [expected-match (new-match-fn)
                       match-id       (:id expected-match)
                       post-request   (build-post-req expected-match)
                       get-request    (build-request :get match-id)

                       handler        (-> handler (liberator.dev/wrap-trace :header))
                       post-response  (-> post-request handler)
                       get-response   (-> get-request handler)
                       actual-match   (-> get-response :body edn/read-string)
                       del-response   (-> (build-request :delete match-id) handler)
                       get-after-del  (-> get-request handler)]

                   (testing "POST succeed"
                     (is (= 201 (:status post-response))))
                   (testing "GET after POST"
                     (is (= 200 (:status get-response)))
                     (is (= match-id (:match/id actual-match))))
                   (testing "DELETE"
                     (is (= 204 (:status del-response))))
                   (testing "GET after DELETE"
                     (is (= 404 (:status get-after-del)))))))
             (testing "validation of POST: "
               (let [build-match-with-scores
                     (fn [s1 s2]
                       (-> (new-match-fn)
                           (update-in [:team1 :score] (constantly s1))
                           (update-in [:team2 :score] (constantly s2))))

                     create-post-request
                     (fn [{:keys [id] :as match}]
                       (-> (build-request :post id)
                           (mockr/header :content-type "application/edn")
                           (update-in [:body]
                                      (fn [_] (prn-str match)))))

                     status-of-reporting-match-with-scores
                     (fn [s1 s2]
                       (-> (build-match-with-scores s1 s2)
                           create-post-request
                           handler
                           :status))]
                 (testing "with invalid scores, it should fail with 'bad request'."
                   (are [s1 s2] (= 400 (status-of-reporting-match-with-scores s1 s2))
                        nil nil
                        10  nil
                        nil 10
                        11  10
                        12  10
                        0   0
                        -1  0
                        -1  10
                        9   10
                        -1  -1))))))))))

  (testing "GET '/api/matchup':"
    (let [app (h/app-with-memory-db)
          handler (:ring-handler app)
          base-request (mockr/request :get "/api/matchup")

          db (:database app)
          [p1 p2 p3 p4 reporter] (map (partial h/create-dummy-player db) ["p1" "p2" "p3" "p4" "rep"])
          league (h/create-dummy-league db "somekindaleague")
          matches (vec (for [index (range 8)
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

          _ (doall (map (fn [m] (d/create-match! db m)) matches))

          build-request-with-players (fn [& players]
                                       (let [query-params (apply merge (map (fn [player index]
                                                                              {(keyword (str "player" (inc index)))
                                                                               (:id player)})
                                                                            players
                                                                            (range)))]
                                         (mockr/query-string base-request query-params)))]
      (test-route-for-etag-with-database-transaction handler
                                                     (build-request-with-players p1 p2 p3 p4)
                                                     (:database app))

      (testing "with 4 distinct players, OK"
        (is (= 200 (-> (build-request-with-players p1 p2 p3 p4) handler :status))))

      (testing "with 4 distinct players, returns 3 results"
        (is (= 3 (-> (build-request-with-players p1 p2 p3 p4) handler :body edn/read-string count))))

      (testing "with 5 distinct players, returns 15 results"
        (is (= 15 (-> (build-request-with-players p1 p2 p3 p4 reporter) handler :body edn/read-string count))))

      (testing "any valid result, contains expected keys"
        (is (= #{:pos-expected
                 :pos-players
                 :neg-expected
                 :neg-players
                 :expected-diff
                 :expected-sortable
                 :pos-rating-diff
                 :neg-rating-diff}
               (->> (build-request-with-players p1 p2 p3 p4) handler :body edn/read-string
                    (mapcat keys) set))))

      (testing "with 5 distinct players, OK"
        (is (= 200 (-> (build-request-with-players p1 p2 p3 p4 reporter) handler :status))))

      (testing "with 4 players (two duplicate), 400"
        (is (= 400 (-> (build-request-with-players p1 p2 p3 p1) handler :status))))

      (testing "with 3 distinct players, 400"
        (is (= 400 (-> (build-request-with-players p1 p2 p3) handler :status))))

      (testing "with no query, 400"
        (is (= 400 (-> base-request handler :status))))))

  (testing "GET '/api/about/software':"
    (let [app (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/about/software")]
      (test-route-for-supported-media-types handler request)
      (test-route-to-not-include-etag handler request)
      (is (not (empty? (-> request handler :body))))))

  (testing "GET '/api/about/version':"
    (let [app (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/about/version")]
      (test-route-for-supported-media-types handler request)
      (test-route-to-not-include-etag handler request)
      (is (not= "" (-> request handler :body)))))

  (testing "GET '/api/auth':"
    (let [app  (h/app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/auth")]
      (test-route-for-supported-media-types handler request)
      (test-route-to-not-include-etag handler request)

      (with-redefs [foosball.auth/current-auth (constantly nil)]
        (testing "with no auth"
          (let [response (-> request handler :body edn/read-string)]
            (is (= {:logged-in? false
                    :provider   foosball.auth/provider}
                   response)))))

      (let [firstname "James"
            lastname "Brown"]
        (with-redefs [foosball.auth/current-auth (constantly (util/identity-map firstname lastname))]
          (with-redefs [foosball.auth/has-role? (fn [r?] (= :foosball.auth/user r?))]
            (let [response (-> request handler :body edn/read-string)]
              (testing "with auth as user"
                (is (= {:logged-in? true
                        :user?      true
                        :admin?     false
                        :username   (str firstname " " lastname)}
                       response))
                (is (nil? (:login-form response))))))

          (with-redefs [foosball.auth/has-role? (constantly true)]
            (let [response (-> request handler :body edn/read-string)]
              (testing "with auth as admin"
                (is (= {:logged-in? true
                        :user?      true
                        :admin?     true
                        :username   (str firstname " " lastname)}
                       response))))))))))
