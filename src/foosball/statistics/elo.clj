(ns foosball.statistics.elo
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(def ^:private ^:const
  ten-times-winning-increase-rating
   "It then follows that for each X rating points of advantage over the opponent, the chance of winning is magnified
  ten times in comparison to the opponent's chance of winning."
   (double 500))

(defn expected-score [rating-a rating-b & {:keys [increase-rating-points increase-magnification]
                                           :or {increase-rating-points ten-times-winning-increase-rating
                                                increase-magnification 10}}]
  (let [q  (fn [r] (Math/pow increase-magnification (/ r increase-rating-points)))
        qa (q rating-a)
        qb (q rating-b)
        ea (/ qa (+ qa qb))
        eb (/ qb (+ qa qb))]
    [ea eb]))

(def ^:private ^:const K (double 32))

(defn updated-rating [current-rating actually-scored expected-score]
  (+ current-rating (* K (- actually-scored expected-score))))
