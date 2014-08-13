(ns foosball.test.convert
  (:require-macros [cemerick.cljs.test
                    :refer (is are deftest testing)])
  (:require [foosball.convert :as convert]
            [cemerick.cljs.test :as t]))

(deftest str->int
  (are [x expected]
    (= expected (convert/str->int x))
    "1"   1
    "0"   0
    "-1" -1
    "not a number" nil))

(deftest ->int
  (are [x expected]
    (= expected (convert/->int x))
    "123" 123
    123   123
    "abc" nil))
