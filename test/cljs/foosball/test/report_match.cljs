(ns foosball.test.report-match
  (:require-macros [cemerick.cljs.test
                    :refer (is are deftest testing)])
  (:require [cemerick.cljs.test :as t]
            [foosball.locations.report-match :as report-match]))

(deftest valid-score?
  (are [t1 t2 exp]
    (= exp (report-match/valid-score? t1 t2))
    nil nil nil
    1   0   1))
