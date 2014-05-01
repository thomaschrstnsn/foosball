(ns foosball.test.routes.home
  (:require [foosball.routes.home :refer :all]
            [foosball.models.domains :as d]
            [foosball.util :as util]
            [foosball.test.helpers :as h]
            [foosball.app :as a]
            [foosball.system :as system]
            [foosball.test.response-helpers :as rh]
            [clojure.test :refer :all]
            [ring.mock.request :as mockr]
            [com.stuartsierra.component :as c]
            [clojure.edn :as edn]))

(use-fixtures :each h/only-error-log-fixture)

(defn app-with-memory-db []
  (c/start (a/map->App {:database (h/memory-db)
                        :config-options system/default-config-options})))

(deftest frontpage-route-tests
  (testing "GET /"
    (let [app (app-with-memory-db)
          handler  (:ring-handler app)
          response (handler (mockr/request :get "/"))]
      (is (not= nil response))
      (is (= 200 (:status response)))
      (is (rh/is-without-cookies? response))))

  (testing "GET '/api/players':"
    (let [app     (app-with-memory-db)
          handler (:ring-handler app)
          request (mockr/request :get "/api/players")]
      (testing "without any players in the database"
        (is (= '() (-> request handler :body edn/read-string)))
        (is (= "application/edn;charset=UTF-8" (-> request handler :headers (get "Content-Type")))))
      (testing "with player in the database"
        (let [db     (:database app)
              name   "thomas"
              openid "http://example.org/openid"
              id     (h/make-uuid)
              _      (d/create-player! db id name openid)]
          (is (= {:role :user
                  :active true
                  :name name
                  :id id} (-> request handler :body edn/read-string first))))))))
