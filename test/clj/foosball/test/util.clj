(ns foosball.test.util
  (:require [clojure.test :refer :all]
            [foosball.util :refer [identity-map]]))

(deftest identity-map-test
  (let [a "Abc"
        b "Bcd"
        c "Cde"]
    (is (= {:a a :b b :c c} (identity-map a b c)))
    (is (= {} (identity-map)))))
