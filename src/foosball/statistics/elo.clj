(ns foosball.statistics.elo
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(def ^:private
  ten-times-winning-increase-rating
   "It then follows that for each X rating points of advantage over the opponent, the chance of winning is magnified
  ten times in comparison to the opponent's chance of winning."
   400.0)

(defn expected-score [rating-a rating-b]
  (let [qa (Math/pow 10 (/ rating-a ten-times-winning-increase-rating))
        qb (Math/pow 10 (/ rating-b ten-times-winning-increase-rating))
        ea (/ qa (+ qa qb))
        eb (/ qb (+ qa qb))]
    [ea eb]))

(def ^:private K 32)

(defn updated-rating [current-rating actually-scored expected-score]
  (+ current-rating (* K (- actually-scored expected-score))))
