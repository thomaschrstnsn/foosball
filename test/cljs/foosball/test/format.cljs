(ns foosball.test.format
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [foosball.format :as sut]))

(deftest format-value
  (testing "it does the basics"
    (is (= [:div nil "0"] (sut/format-value 0)))
    (is (= [:div {:class "text-success"} "1" (sut/format-value 1)]))
    (is (= [:div {:class "text-danger"} "-1" (sut/format-value -1)]))))

(deftest format-match-percentage
  (testing "winners"
    (is (= [:div {:class "text-success"} "51.0%"] (sut/format-match-percentage true 51.001)))
    (is (= [:div {:class "text-danger"} "49.9%"] (sut/format-match-percentage true 49.9))))
  (testing "loosers"
    (is (= [:div {:class "text-danger"}  "51.0%"] (sut/format-match-percentage false 51.001)))
    (is (= [:div {:class "text-success"} "49.9%"] (sut/format-match-percentage false 49.9)))))
