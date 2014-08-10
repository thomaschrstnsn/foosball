(ns foosball.test.format
  (:require-macros [cemerick.cljs.test
                    :refer (is are deftest testing)])
  (:require [foosball.format :as sut]
            [foosball.routes :as routes]
            [cljs-uuid-utils :as uuid]
            [foosball.locations.report-match :as rm]
            [cemerick.cljs.test]))

;; for now only one test namespace is executed?!!
(deftest clojure-core-set
  (testing "sets are functions to lookup their elements"
    (let [range-ten-set (set (range 10))]
      (is (= nil (range-ten-set -1)))
      (is (= 1 (range-ten-set 1)))
      (is (= 2 (range-ten-set 2)))))
  (testing "score validation"
    (testing "valid-score?"
      (are [s1 s2 exp]
        (= exp (rm/valid-score? s1 s2))
        0   nil true
        10  nil true
        10  10  false))))

(deftest style-value
  (is (= [:div nil "0"] (sut/style-value 0)))
  (is (= [:div {:class "text-success"} "1" (sut/style-value 1)]))
  (is (= [:div {:class "text-danger"} "-1" (sut/style-value -1)])))

(deftest style-score
  (is (= [:div {:class "text-success"} "10"] (sut/style-score 10)))
  (is (= [:div {:class "text-success"} "11"] (sut/style-score 11)))
  (is (= [:div {:class "text-danger"}   "9"] (sut/style-score 9)))
  (is (= [:div {:class "text-danger"}   "0"] (sut/style-score 0))))

(deftest style-rating
  (is (= [:div {:class "text-success"} "1500.1"] (sut/style-rating 1500.1)))
  (is (= [:div {:class "text-danger"}  "1499.9"] (sut/style-rating 1499.9))))

(deftest style-form
  (is (= [[:span {:class "text-danger"}  "L"]
          [:span {:class "text-success"} "W"]
          [:span {:class "text-success"} "W"]] (sut/style-form :w :l [:l :w :w])))
  (is (= [[:span {:class "text-success"} "W"]
          [:span {:class "text-danger"}  "L"]] (sut/style-form true false [true false]))))

(deftest style-match-percentage
  (testing "winners"
    (is (= [:div {:class "text-success"} "51.0%"] (sut/style-match-percentage true 51.001)))
    (is (= [:div {:class "text-danger"} "49.9%"] (sut/style-match-percentage true 49.9))))
  (testing "loosers"
    (is (= [:div {:class "text-danger"}  "51.0%"] (sut/style-match-percentage false 51.001)))
    (is (= [:div {:class "text-success"} "49.9%"] (sut/style-match-percentage false 49.9)))))

(deftest format-date
  (is (= "24-11-2003" (sut/format-date #inst "2003-11-24"))))

(deftest format-player-link
  (let [my-player-id (uuid/make-random-uuid)
        players [{:id (uuid/make-random-uuid) :name "Other player"}
                 {:id my-player-id :name "My player"}]]
    (with-redefs [routes/player-log-path (constantly "fixed-link")]
      (is (= [:a {:href "fixed-link"} "My player"]
             (sut/format-player-link players "My player"))))))
