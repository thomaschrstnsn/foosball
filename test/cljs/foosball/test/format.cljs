(ns foosball.test.format
  (:require-macros [cemerick.cljs.test
                    :refer (is are deftest testing)])
  (:require [cemerick.cljs.test :as t]
            [foosball.format :as format]
            [foosball.routes :as routes]
            [cljs-uuid-utils :as uuid]))

(deftest style-value
  (is (= [:div nil "0"] (format/style-value 0)))
  (is (= [:div {:class "text-success"} "1" (format/style-value 1)]))
  (is (= [:div {:class "text-danger"} "-1" (format/style-value -1)])))

(deftest style-score
  (is (= [:div {:class "text-success"} "10"] (format/style-score 10)))
  (is (= [:div {:class "text-success"} "11"] (format/style-score 11)))
  (is (= [:div {:class "text-danger"}   "9"] (format/style-score 9)))
  (is (= [:div {:class "text-danger"}   "0"] (format/style-score 0))))

(deftest style-rating
  (is (= [:div {:class "text-success"} "1500.1"] (format/style-rating 1500.1)))
  (is (= [:div {:class "text-danger"}  "1499.9"] (format/style-rating 1499.9))))

(deftest style-form
  (is (= [[:span {:class "text-danger"}  "L"]
          [:span {:class "text-success"} "W"]
          [:span {:class "text-success"} "W"]] (format/style-form :w :l [:l :w :w])))
  (is (= [[:span {:class "text-success"} "W"]
          [:span {:class "text-danger"}  "L"]] (format/style-form true false [true false]))))

(deftest style-match-percentage
  (testing "winners"
    (is (= [:div {:class "text-success"} "51.0%"] (format/style-match-percentage true 51.001)))
    (is (= [:div {:class "text-danger"} "49.9%"] (format/style-match-percentage true 49.9))))
  (testing "loosers"
    (is (= [:div {:class "text-danger"}  "51.0%"] (format/style-match-percentage false 51.001)))
    (is (= [:div {:class "text-success"} "49.9%"] (format/style-match-percentage false 49.9)))))

(deftest format-player-link
  (let [my-player-id (uuid/make-random-uuid)
        players [{:id (uuid/make-random-uuid) :name "Other player"}
                 {:id my-player-id :name "My player"}]]
    (with-redefs [routes/player-log-path (constantly "fixed-link")]
      (is (= [:a {:href "fixed-link"} "My player"]
             (format/format-player-link players "My player"))))))
