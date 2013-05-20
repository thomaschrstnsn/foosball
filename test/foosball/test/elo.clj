(ns foosball.test.elo
  (:use midje.sweet foosball.statistics.elo))

(tabular "about expected-score"
         (fact "it should match the wikipedia example"
               (expected-score 1613 ?opponent-rating) => (just (roughly ?expected-score-a 0.00049)
                                                               anything))
         ?opponent-rating ?expected-score-a
         1609             0.506
         1477             0.686
         1388             0.785
         1586             0.539
         1720             0.351)

(tabular "Table fact of expected-score"
         (fact "it should always sum to 1"
               (apply + (expected-score ?rating-a ?rating-b)) => (exactly 1.0))
         ?rating-a  ?rating-b
         1613       1
         1613       10000
         1613       1613
         1          1000
         1000       1
         1          1
         3000       3000)

(tabular "about updated-rating"
         (fact "it should match the wikipedia example"
               (updated-rating 1613 ?actual-score 2.867) => (roughly ?expected-rating 0.49))
         ?actual-score ?expected-rating
         0             1521
         1             1553
         2.5           1601
         3             1617
         4             1649
         5             1681)
