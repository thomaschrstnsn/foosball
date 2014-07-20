(ns foosball.test.elo
  (:require [foosball.statistics.elo :refer :all]
            [clojure.test :refer :all]
            [foosball.test.helpers :as h]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(deftest expected-score-tests
  (testing "it should match the wikipedia example"
    (let [scores= (fn [exp act] (h/roughly= exp act 0.00049))
          wikipedia-ratings-increase-value (double 400)
          compute-score-a (fn [opponent-rating]
                            (first (expected-score 1613
                                                   opponent-rating
                                                   :increase-rating-points wikipedia-ratings-increase-value)))]
      (are [opponent-rating expected-score-a]
        (scores= expected-score-a (compute-score-a opponent-rating))
        1609 0.506
        1477 0.686
        1388 0.785
        1586 0.539
        1720 0.351))))

(deftest updated-rating-tests
  (testing "updated-rating"
    (testing "it should match the wikipedia example"
      (let [ratings= (fn [exp act] (h/roughly= exp act 0.49))]
        (are [actual-score expected-rating]
          (ratings= expected-rating (updated-rating 1613 actual-score 2.867))
         0   1521
         1   1553
         2.5 1601
         3   1617
         4   1649
         5   1681)))))

(defspec expected-scores-always-sum-to-1
  10000
  (prop/for-all [rating-a (gen/such-that pos? gen/nat)
                 rating-b (gen/such-that pos? gen/nat)]
                (h/roughly= 1.0 (apply + (expected-score rating-a rating-b)) 0.0000000001)))
