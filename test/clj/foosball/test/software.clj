(ns foosball.test.software
  (:require [foosball.software :as sut]
            [clojure.test :refer :all]
            [cfg.current :refer [project]]
            [schema.test :as schema.test]))

(use-fixtures :once schema.test/validate-schemas)

(deftest software-dependencies
  (testing "It works"
    (is (= {:version "1.6.0" :name "Clojure" :url "http://clojure.org"}
           (first (sut/software-dependencies project))))))
