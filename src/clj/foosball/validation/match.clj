(ns foosball.validation.match)

(defn- pick-scores [{:keys [team1 team2]}]
  (mapv :score [team1 team2]))

(defn- flatten-distinct-filter [& vs]
  (->> vs
       flatten distinct
       (filterv (comp not nil?))))

(defn- valid-single-score? [n] (when n (<= 0 n 11)))

(defn validate-scores [[team1 team2 :as both]]
  (let [nil-filtered       (filter (comp not nil?) both)
        both-invalid       [[:team1score false] [:team2score false]]
        single-validations [[:team1score (if (nil? team1) nil (valid-single-score? team1))]
                            [:team2score (if (nil? team2) nil (valid-single-score? team2))]]
        both-validations   (when (= 2 (count nil-filtered))
                             (let [greatest (apply max nil-filtered)
                                   least    (apply min nil-filtered)]
                               (->> [(when-not (<= 10 greatest) both-invalid)
                                     (when-not (<= 2 (- greatest least)) both-invalid)
                                     (when     (and (= 11 greatest) (not= 9 least)) both-invalid)
                                     (when     (= team1 team2) both-invalid)]
                                    (filter (complement nil?))
                                    first)))]
    (->> (concat single-validations both-validations)
         (map (fn [[k v]] {k v}))
         (apply merge))))

(defn- pick-players [{:keys [team1 team2]}]
  (let [{t1player1 :player1 t1player2 :player2} team1
        {t2player1 :player1 t2player2 :player2} team2]
    [t1player1 t1player2 t2player1 t2player2]))

(defn- pick-matchdate [{:keys [matchdate]}]
  matchdate)

(defn validate-players [players]
  (let [mapped      (apply assoc {} (interleave [:team1player1 :team1player2
                                                 :team2player1 :team2player2]
                                                players))
        more-than-1 (->> players
                         (filter (comp not nil?))
                         frequencies
                         (filter #(< 1 (second %)))
                         (map first))

        nils        (->> mapped
                         (filter (comp nil? second)))

        non-unique  (->> mapped
                         (filter (fn [[k v]] (some #{v} more-than-1)))
                         (map (fn [[k v]] [k false])))]
    (->> (concat nils non-unique)
         (map (fn [[k v]] {k v}))
         (apply (partial merge {:team1player1 true :team1player2 true
             :team2player1 true :team2player2 true})))))

(defn validate-matchdate [md]
  {:matchdate (when md
                (if (= :invalid-matchdate md)
                  false
                  true))})

(defn validate-report [report]
  (merge (->> report pick-scores    validate-scores)
         (->> report pick-players   validate-players)
         (->> report pick-matchdate validate-matchdate)))
