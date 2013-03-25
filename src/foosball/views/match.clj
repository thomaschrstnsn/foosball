(ns foosball.views.match
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use  hiccup.form ring.util.response
         [hiccup.def :only [defhtml]]
         [hiccup.element :only [link-to]]
         [hiccup.page :only [html5 include-js include-css]]
         [foosball.util :only [format-time parse-time parse-int]]))

(defn- team-controls [kw team-num values validation-errors]
  (let [prefix  (name kw)
        idp1    (str prefix "player" 1)
        idp2    (str prefix "player" 2)
        idscore (str prefix "score")]
    [:div
     [:h3 (str "Team " team-num)]

     [:div.control-group
      [:label.control-label {:for idp1} (str "Player" 1)]
      [:div.controls [:input.input-small  {:id idp1 :name idp1 :type "text" :placeholder "Player name"
                                           :value (:player1 values)}]]]
     [:div.control-group
      [:label.control-label {:for idp2} (str "Player" 2)]
      [:div.controls [:input.input-small  {:id idp2 :name idp2 :type "text" :placeholder "Player name"
                                           :value (:player2 values)}]]]
     [:div.control-group
      [:label.control-label {:for idscore} "Score"]
      [:div.controls [:input.input-small {:id idscore :name idscore :type "number" :placeholder "0"
                                          :min "0" :max "11"
                                          :value (:score values)}]]]]))

(defn form [& [{:keys [team1 team2 validation-errors matchdate]
                :or {matchdate (new java.util.Date)}}]]
  (html5
   [:form.form-horizontal {:action "/match" :method "POST"}
    (team-controls :team1 1 team1 validation-errors)
    (team-controls :team2 2 team2 validation-errors)

    [:div.control-group
     [:div.controls [:input.input-small {:id "matchdate" :name "matchdate" :type "date" :value (format-time matchdate)}]]]

    [:div.control-group
     [:div.controls [:button.btn.btn-primary {:type "submit" :value "Report"} "Report"]]] ]))

(defn parse-form [p]
  {:matchdate       (-> p :matchdate parse-time)
   :team1 {:player1 (p :team1player1)
           :player2 (p :team1player2)
           :score   (-> p :team1score parse-int)}
   :team2 {:player1 (p :team2player1)
           :player2 (p :team2player2)
           :score   (-> p :team2score parse-int)}})

(defn- pick-scores [{:keys [team1 team2]}]
  (->> [team1 team2]
       (map :score)
       (apply vector)))

(defn- flatten-distinct-filter [vs]
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
  (->> [team1 team2]
       (map #{:player1 :player2})
       (apply vector)))

(defn- validate-players [[team1p1 team1p2 team2p1 team2p2 :as players]] nil)

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
