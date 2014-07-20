(ns foosball.test.validation-match
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [conjure.core :refer [stubbing]]
            [foosball.validation.match :refer :all]
            [foosball.test.helpers :as h]
            [foosball.util :as util]))

(def valid-losing-to-ten-scores (gen/choose 0 8))

(def invalid-scores
  (let [valid-scores (set (range 0 12))]
    (gen/such-that (fn [n] (not (valid-scores n))) gen/int 100)))

(defspec invalid-team1-scores
  (prop/for-all [input (gen/tuple invalid-scores gen/int)]
                (= false (:team1score (validate-scores input)))))

(defspec nil-team1-scores
  (prop/for-all [input (gen/tuple (gen/elements [nil]) gen/int)]
                (= nil (:team1score (validate-scores input)))))

(defspec invalid-team2-scores
  (prop/for-all [input (gen/tuple gen/int invalid-scores)]
                (= false (:team2score (validate-scores input)))))

(defspec nil-team2-scores
  (prop/for-all [input (gen/tuple gen/int (gen/elements [nil]))]
                (= nil (:team2score (validate-scores input)))))

(defspec both-scores-invalid
  (prop/for-all [input (gen/tuple invalid-scores invalid-scores)]
                (= {:team1score false
                    :team2score false} (validate-scores input))))

(defspec valid-team1-losing-to-ten-scores
  (prop/for-all [input (gen/tuple valid-losing-to-ten-scores (gen/elements [10]))]
                (= {:team1score true
                    :team2score true}
                   (validate-scores input))))

(defspec valid-team2-losing-to-ten-scores
  (prop/for-all [input (gen/tuple (gen/elements [10]) valid-losing-to-ten-scores)]
                (= {:team1score true
                    :team2score true}
                   (validate-scores input))))

(defspec scores-validity-is-reversible-the-same 10000
  (prop/for-all [input (gen/tuple gen/int gen/int)]
                (let [reverse-input  (-> input reverse vec)
                      reverse-output (fn [x] {:team1score (:team2score x)
                                             :team2score (:team1score x)})]
                  (= (validate-scores input)
                     (reverse-output (validate-scores reverse-input))))))

(deftest enumerated-validate-scores-tests
  (testing "truth table"
    (are [input expected]
      (let [[exp-score1 exp-score2] expected]
        (= {:team1score exp-score1 :team2score exp-score2}
           (validate-scores input)))
      [10  8] [true  true]
      [10  9] [false false]
      [11 10] [false false]
      [11  9] [true  true]
      [12  9] [false true]
      [nil nil] [nil nil])))

(h/with-private-fns [foosball.validation.match [pick-players]]
  (defspec picking-players-specs
    (prop/for-all [ps (gen/tuple gen/int gen/int gen/int gen/int)]
                  (let [[p1 p2 p3 p4] ps
                        input {:team1 {:player1 p1 :player2 p2}
                               :team2 {:player1 p3 :player2 p4}}]
                    (= ps (pick-players input)))))
  (deftest pickick-players-tests
    (is (= [nil nil nil nil] (pick-players {:team1 nil :team2 nil})))))

(deftest validate-players-tests
  (let [all-valid {:team1player1 true :team1player2 true
                   :team2player1 true :team2player2 true}]
    (testing "unique non-nil players are valid"
      (is (= all-valid (validate-players [1 2 3 4]))))
    (testing "nil players are invalid"
      (is (= (merge all-valid {:team1player1 nil}) (validate-players [nil   2   3   4])))
      (is (= (merge all-valid {:team1player2 nil}) (validate-players [  1 nil   3   4])))
      (is (= (merge all-valid {:team2player1 nil}) (validate-players [  1   2 nil   4])))
      (is (= (merge all-valid {:team2player2 nil}) (validate-players [  1   2   3 nil])))
      (is (= {:team1player1 nil :team1player2 nil
              :team2player1 nil :team2player2 nil} (validate-players [nil nil nil nil]))))
    (testing "non-unique players are invalid"
      (is (= (merge all-valid {:team1player1 false :team1player2 false}) (validate-players [1 1 2 3])))
      (is (= (merge all-valid {:team1player2 false :team2player1 false}) (validate-players [1 2 2 3])))
      (is (= (merge all-valid {:team2player1 false :team2player2 false}) (validate-players [1 2 3 3])))
      (is (= (merge all-valid {:team1player1 false :team2player2 false}) (validate-players [1 2 3 1]))))
    (testing "some combinations that are invalid"
      (is (= {:team1player1 false :team1player2 false
              :team2player1 nil   :team2player2 nil} (validate-players [1 1 nil nil]))))))

(deftest validate-matchdate-tests
  (are [input output]
    (= {:matchdate output} (validate-matchdate input))
    :invalid-matchdate false
    nil                nil
    :something         true))

(deftest validate-report-tests
  (let [all-keys     [:team1player1 :team2player1
                      :team1player2 :team2player2
                      :team1score   :team2score
                      :matchdate]
        same-vals-fn (fn [val] (apply assoc {} (interleave all-keys
                                                          (repeatedly (constantly val)))))]
    (testing "a valid report should should only contain true values"
      (is (= (same-vals-fn true)
             (validate-report {:team1 {:player1 1 :player2 2 :score 8}
                               :team2 {:player1 3 :player2 4 :score 10}
                               :matchdate "2013-07-15"})))
      (is (= (same-vals-fn true)
             (validate-report {:team1 {:player1 1 :player2 2 :score 8}
                               :team2 {:player1 3 :player2 4 :score 10}
                               :matchdate (util/parse-date "2013-07-15")}))))
    (testing "an empty report should only contain nil values"
      (is (= (same-vals-fn nil) (validate-report {})))))
  (testing "validated reports is merge of maps from validation functions"
    (let [report {:team1 :...team1... :team2 :...team2... :matchdate :...matchdate...}]
      (stubbing [validate-players   {:vp :...vp...}
                 validate-scores    {:vs :...vs...}
                 validate-matchdate {:vmd :...vmd...}]
                (is (= {:vp :...vp... :vs :...vs... :vmd :...vmd...}
                       (validate-report report)))))))
