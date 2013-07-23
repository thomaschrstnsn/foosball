(ns foosball.validation.match)

(defn- pick-scores [{:keys [team1 team2]}]
  (->> [team1 team2]
       (map :score)
       (apply vector)))

(defn- flatten-distinct-filter [& vs]
  (->> vs
       flatten distinct
       (filter (comp not nil?))
       (apply vector)))

(defn- valid-single-score? [n] (when n (<= 0 n 11)))

(defn- validate-scores [[team1 team2 :as both]]
  (let [nil-filtered (filter (comp not nil?) both)]
    (if (empty? nil-filtered)
      [:team1 :team2]
      (let [greatest (apply max nil-filtered)
            least    (apply min nil-filtered)]
        (flatten-distinct-filter
         [(when-not (valid-single-score? team1) [:team1])
          (when-not (valid-single-score? team2) [:team2])
          (when (= 2 (count nil-filtered))
            [(when-not (<= 10 greatest) [:team1 :team2])
             (when-not (<= 2 (- greatest least)) [:team1 :team2])
             (when     (and (= 11 greatest) (not= 9 least)) [:team1 :team2])
             (when     (= team1 team2) [:team1 :team2])])])))))

(defn- pick-players [{:keys [team1 team2]}]
  (let [{t1player1 :player1 t1player2 :player2} team1
        {t2player1 :player1 t2player2 :player2} team2]
    [t1player1 t1player2 t2player1 t2player2]))

(defn- validate-players [[team1p1 team1p2 team2p1 team2p2 :as players]]
  (let [mapped (apply assoc {} (interleave [:team1player1 :team1player2
                                            :team2player1 :team2player2]
                                           players))
        non-unique (->> players
                        (filter (comp not nil?))
                        frequencies
                        (filter #(< 1 (second %)))
                        (map first))]
    (flatten-distinct-filter
     ;; nils
     (->> mapped
          (filter (comp nil? second))
          (map first))
     ;; non-unique
     (->> mapped
          (filter (fn [[k v]] (some #{v} non-unique)))
          (map first)))))

(defn validate-report [r]
  (->> {:scores  (fn [x] (->> x pick-scores  validate-scores))
        :players (fn [x] (->> x pick-players validate-players))}

       ; apply validations
       (map (fn [[k f]] [k (f r)]))

       ; filter empty
       (filter (fn [[k vs]] ((complement empty?) vs)))

       ; into map and merge
       (map (fn [[k vs]] {k vs}))
       (apply merge)

       (assoc {} :validation-errors)
       (into r)))
