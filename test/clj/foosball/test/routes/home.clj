(ns foosball.test.routes.home
  (:require [foosball.models.domains :as d]
            [foosball.test.helpers :as h]
            [foosball.test.response-helpers :as rh]
            [clojure.test :refer :all]
            [ring.mock.request :as mockr]
            [clojure.edn :as edn]
            [schema.test :as schema.test]))

(use-fixtures :each h/only-error-log-fixture)
(use-fixtures :once schema.test/validate-schemas)

(deftest frontpage-route-tests
  (testing "GET /"
    (let [app      (h/app-with-memory-db)
          handler  (:ring-handler app)
          response (handler (mockr/request :get "/"))]
      (is (not= nil response))
      (is (= 200 (:status response)))
      (is (rh/is-without-cookies? response)))))
