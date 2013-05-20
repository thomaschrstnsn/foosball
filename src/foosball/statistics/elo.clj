(ns foosball.statistics.elo
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn expected-score [rating-a rating-b]
  (let [ten-r (double 400)
        qa (Math/pow 10 (/ rating-a ten-r))
        qb (Math/pow 10 (/ rating-b ten-r))
        ea (/ qa (+ qa qb))
        eb (/ qb (+ qa qb))]
    [ea eb]))

(def ^:private K 32)

(defn updated-rating [current-rating actually-scored expected-score]
  (+ current-rating (* K (- actually-scored expected-score))))
