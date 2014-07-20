(ns foosball.test.util
  (:require [clojure.test :refer :all]
            [foosball.util :refer [symbols-as-map]]))

(deftest symbols-as-map-test
  (let [a "Abc"
        b "Bcd"
        c "Cde"]
    (is (= {:a a :b b :c c} (symbols-as-map a b c)))
    (is (= {} (symbols-as-map)))))
