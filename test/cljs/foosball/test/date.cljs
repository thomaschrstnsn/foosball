(ns foosball.test.date
  (:require-macros [cemerick.cljs.test
                    :refer (is are deftest testing)])
  (:require [foosball.date :as date]
            [cemerick.cljs.test :as t]
            [cljs-time.coerce :as dc]
            [cljs-time.core :as dt]))

(defn expected-date [& {:keys [year month day]}]
  {:year year :month month :day day})

(defn date-time=expected? [dt {:keys [year month day] :as expected}]
  (if dt
    (and (= (dt/year  dt) year)
         (= (dt/month dt) month)
         (= (dt/day   dt) day))
    (and (nil? dt) (nil? expected))))

(deftest str->date
  (testing "exact matches"
    (are [x expected]
      (date-time=expected? (date/str->date x) expected)
      "12/8/1980"  (expected-date :year 1980 :month 8 :day 12)
      "14/8/2014"  (expected-date :year 2014 :month 8 :day 14)
      "14/08/2014" (expected-date :year 2014 :month 8 :day 14)
      "01/01/1970" (expected-date :year 1970 :month 1 :day 1))))

(deftest roundtripping
  (testing "roundtripping strings"
    (are [s]
      (= (-> s date/str->date date/->str) s)
      "12/08/1980"
      "01/01/1970"
      "31/12/2048"))
  (testing "roundtripping dates"
    (are [d expected]
      (date-time=expected? (-> d date/->str date/str->date) expected)
      #inst "1980-08-12" (expected-date :year 1980 :month 8  :day 12)
      #inst "2048-12-31" (expected-date :year 2048 :month 12 :day 31))))

(deftest ->str
  (are [x expected]
    (= (date/->str x) expected)
    #inst "2003-11-24" "24/11/2003"
    #inst "2014-01-01" "01/01/2014"
    #inst "1970-01-01" "01/01/1970"))
