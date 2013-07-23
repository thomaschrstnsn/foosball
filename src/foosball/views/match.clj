(ns foosball.views.match
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use  hiccup.form ring.util.response
         [hiccup.def :only [defhtml]]
         [hiccup.element :only [link-to]]
         [hiccup.page :only [html5 include-js include-css]]
         [foosball.util]))

(defn- validation-error? [validation-errors typekw kw]
  (->> validation-errors
       typekw
       (some #{kw})))

(defn- players-select [id players & [selected]]
  [:select.input-medium {:id id :name id}
   [:option {:value "nil" :disabled "disabled" :selected "selected"} "Pick a player"]
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
    [:div.span6.well
     [:h2 (str "Team " team-num ":")]

     [:div.control-group
      (when (validation-error? validation-errors :players (keyword idp1)) error-class)
      [:label.control-label {:for idp1} (str "Player " 1)]
      [:div.controls (players-select idp1 players player1)]]

     [:div.control-group
      (when (validation-error? validation-errors :players (keyword idp2)) error-class)
      [:label.control-label {:for idp2} (str "Player " 2)]
      [:div.controls (players-select idp2 players player2)]]

     [:div.control-group
      (when (validation-error? validation-errors :scores kw) error-class)
      [:label.control-label {:for idscore} "Score"]
      [:div.controls [:input.input-mini {:id idscore :name idscore :type "number" :placeholder "0"
                                          :min "0" :max "11"
                                          :value score}]]]]))

(defn- filter-active-players [players]
  (filter :active players))

(defn form [players & [{:keys [team1 team2 validation-errors matchdate]
                        :or {matchdate (java.util.Date.)}}]]
  (let [active-players (filter-active-players players)]
    (html5
     [:h1 "Report Match Result"]
     [:p.lead
      "A match winner is the first team to reach ten goals while atleast two goals ahead of the opposing team." [:br]
      "In case of tie-break, report 11-9 or 9-11."]
     [:form.form-horizontal {:action "/report/match" :method "POST"}
      [:div.row-fluid
       (team-controls :team1 1 team1 active-players validation-errors)
       (team-controls :team2 2 team2 active-players validation-errors)]

      [:div.row-fluid
       [:div.offset2.span8.well
        [:h2 "Match:"]
        [:div.control-group
         [:label.control-label {:for "matchdate"} "Date played"]
         [:div.controls [:input.input-medium {:id "matchdate" :name "matchdate"
                                       :type "date" :value (format-datetime matchdate)}]]]]]
      [:div.row-fluid
       [:div.control-group
        [:button.btn.btn-primary.btn-large.btn-block.span8.offset2
         {:type "submit" :value "Report"} "Report Match Result " [:i.icon-ok.icon-white]]]]])))

(defn- render-match [{:keys [matchdate team1 team2 id]} players & {:keys [admin] :or {admin false}}]
  (let [[t1p1 t1p2 t1score] (map team1 [:player1 :player2 :score])
        [t2p1 t2p2 t2score] (map team2 [:player1 :player2 :score])]
    [:tr
     [:td [:div.text-center (format-datetime matchdate)]]
     [:td [:div.text-center (render-team players [t1p1 t1p2])]]
     [:td [:div.text-center (format-score t1score)]]
     [:td [:div.text-center (render-team players [t2p1 t2p2])]]
     [:td [:div.text-center (format-score t2score)]]
     (when admin [:td [:button.btn.btn-danger {:type "submit" :name "matchid" :value id} "Remove!"]])]))

(defn match-table-data [matches players & {:keys [admin] :or {admin false}}]
  [:table.table.table-hover.table-condensed [:caption [:h1 "Played Matches"]]
    [:thead
     [:tr
      [:th [:div.text-center "Date played"]]
      [:th [:div.text-center "Team 1"]]
      [:th [:div.text-center "Score"]]
      [:th [:div.text-center "Team 2"]]
      [:th [:div.text-center "Score"]]
      (when admin
        [:th ""])]]
    [:tbody
     (->> matches
          reverse
          (map (fn [match] (render-match match players :admin admin))))]])

(defn table [matches players]
  (html5
   (auto-refresh-page)
   (match-table-data matches players)))

(defn parse-form [p]
  {:matchdate       (-> p :matchdate    parse-time)
   :team1 {:player1 (-> p :team1player1 parse-id)
           :player2 (-> p :team1player2 parse-id)
           :score   (-> p :team1score   parse-id)}
   :team2 {:player1 (-> p :team2player1 parse-id)
           :player2 (-> p :team2player2 parse-id)
           :score   (-> p :team2score   parse-id)}})
