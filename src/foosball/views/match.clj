(ns foosball.views.match
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use  hiccup.form ring.util.response
         [hiccup.def :only [defhtml]]
         [hiccup.element :only [link-to]]
         [hiccup.page :only [html5 include-js include-css]]
         [foosball.util :only [format-time parse-time parse-id]]))

(defn- validation-error? [validation-errors typekw kw]
  (->> validation-errors
       typekw
       (some #{kw})))

(defn- players-select [id players & [selected]]
  [:select.input-small {:id id :name id}
   [:option {:value "nil" } "Pick a player"]
   (->> players
        (map (fn [{:keys [id name]}]
               [:option (merge {:value id}
                               (when (= id selected) {:selected "selected"}))
                name])))])

(defn- team-controls [kw team-num {:keys [player1 player2 score]} players validation-errors]
  (let [prefix  (name kw)
        idp1    (str prefix "player" 1)
        idp2    (str prefix "player" 2)
        idscore (str prefix "score")
        error-class {:class "control-group error"}]
    [:div
     [:h3 (str "Team " team-num)]

     [:div.control-group
      (when (validation-error? validation-errors :players (keyword idp1)) error-class)
      [:label.control-label {:for idp1} (str "Player" 1)]
      [:div.controls (players-select idp1 players player1)]]

     [:div.control-group
      (when (validation-error? validation-errors :players (keyword idp2)) error-class)
      [:label.control-label {:for idp2} (str "Player" 2)]
      [:div.controls (players-select idp2 players player2)]]

     [:div.control-group
      (when (validation-error? validation-errors :scores kw) error-class)
      [:label.control-label {:for idscore} "Score"]
      [:div.controls [:input.input-small {:id idscore :name idscore :type "number" :placeholder "0"
                                          :min "0" :max "11"
                                          :value score}]]]]))

(defn form [players & [{:keys [team1 team2 validation-errors matchdate]
                        :or {matchdate (java.util.Date.)}}]]
  (html5
   [:form.form-horizontal {:action "/match" :method "POST"}
    (team-controls :team1 1 team1 players validation-errors)
    (team-controls :team2 2 team2 players validation-errors)

    [:div.control-group
     [:div.controls [:input.input-small {:id "matchdate" :name "matchdate"
                                         :type "date" :value (format-time matchdate)}]]]

    [:div.control-group
     [:div.controls [:button.btn.btn-primary {:type "submit" :value "Report"} "Report"]]] ]))

(defn render-match [{:keys [matchdate team1 team2]}]
  (let [[t1p1 t1p2 t1score] (map team1 [:player1 :player2 :score])
        [t2p1 t2p2 t2score] (map team2 [:player1 :player2 :score])]
    [:tr
     [:td (format-time matchdate)]
     [:td t1p1]
     [:td t1p2]
     [:td t1score]
     [:td t2p1]
     [:td t2p2]
     [:td t2score]]))

(defn table [matches]
  (html5
   [:table.table.table-hover.table-bordered [:caption [:h2 "Reported Matches"]]
    [:thead
     [:tr
      [:th {:colspan 1} ""]
      [:th {:colspan 3} "Team1"]
      [:th {:colspan 3} "Team2"]]
     [:tr
      [:th "Date played"]
      [:th "Player 1"]
      [:th "Player 2"]
      [:th "Score"]
      [:th "Player 1"]
      [:th "Player 2"]
      [:th "Score"]]]
    [:tbody
     (map render-match matches)]]))

(defn parse-form [p]
  {:matchdate       (-> p :matchdate parse-time)
   :team1 {:player1 (-> p :team1player1 parse-id)
           :player2 (-> p :team1player2 parse-id)
           :score   (-> p :team1score parse-id)}
   :team2 {:player1 (-> p :team2player1 parse-id)
           :player2 (-> p :team2player2 parse-id)
           :score   (-> p :team2score parse-id)}})

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
