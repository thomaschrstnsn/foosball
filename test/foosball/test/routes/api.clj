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
          (testing "Accept header decides result"
            (are [mime-type]
              (= 200 (-> request
                         (mockr/header :accept mime-type)
                         ((libdev/wrap-trace handler :header))
                         :status))
              "application/edn"
              "text/html"
              "application/json"))

          (testing "JSON output is parsable into something similar"
            (is  (= {:role "user"
                     :name name
                     :active true
                     :id (str id)} (-> request
                                       (mockr/header :accept "application/json")
                                       ((libdev/wrap-trace handler :header))
                                       :body
                                       (json/read-str :key-fn keyword)
                                       first)))))))))
