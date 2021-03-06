(ns foosball.views.match
  (:require [foosball.util :as util]
            [hiccup.page :refer [html5]]))

(defn- render-players-select [id players & [selected]]
  [:select.form-control {:id id :name id}
   [:option {:value "nil" :disabled "disabled" :selected "selected"} "Pick a player"]
   (->> players
        (map (fn [{:keys [id name]}]
               [:option (merge {:value id}
                               (when (= id selected) {:selected "selected"}))
                name])))])

(defn- render-team-controls [kw team-num {:keys [player1 player2 score]} players]
  (let [prefix  (name kw)
        idp1    (str prefix "player" 1)
        idp2    (str prefix "player" 2)
        idscore (str prefix "score")]
    [:div.col-lg-5.well.well-lg
     [:h2 (str "Team " team-num ":")]
     [:div.form-group
      [:label.control-label.col-lg-4 {:for idp1} (str "Player " 1)]
      [:div.controls.col-lg-8 (render-players-select idp1 players player1)]]

     [:div.form-group
      [:label.control-label.col-lg-4 {:for idp2} (str "Player " 2)]
      [:div.controls.col-lg-8 (render-players-select idp2 players player2)]]

     [:div.form-group
      [:label.control-label.col-lg-4 {:for idscore} "Score"]
      [:div.controls.col-lg-8
       [:input.form-control {:id idscore :name idscore :type "number" :placeholder "0"
                             :min "0" :max "11" :value score}]]]]))

(defn- filter-active-players [players]
  (filter :active players))

(defn render-match-date [matchdate]
  (let [formated-date (util/format-date (->> matchdate
                                             ((fn [x] (when-not (= :invalid-matchdate x) x)))
                                             ((fnil identity (java.util.Date.)))))]
    [:div.form-group.col-lg-12
     [:div.form-group.pull-right.col-lg-6
      [:label.control-label.col-lg-6 {:for "matchdate"} "Date played"]
      [:div.controls.col-lg-5
       [:input.input-medium.form-control {:id "matchdate" :name "matchdate"
                                          :type "date"
                                          :value formated-date}]]]]))

(defn form [players & [{:keys [team1 team2 matchdate]}]]
  (let [active-players (filter-active-players players)]
    (html5
     [:h1 "Report Match Result"]
     [:p.lead
      "A match winner is the first team to reach ten goals while atleast two goals ahead of the opposing team." [:br]
      "In case of tie-break, report 11-9 or 9-11."]

     [:form.form-horizontal {:action "/report/match" :method "POST"}
      [:div.form-group.col-lg-12
       (render-team-controls :team1 1 team1 active-players)
       [:div.col-lg-2]
       (render-team-controls :team2 2 team2 active-players)]

      (render-match-date matchdate)

      [:div.form-group.col-lg-12
       [:div.form-group.col-lg-5.pull-right
        [:button.btn.btn-primary.btn-lg.btn-block
         {:type "submit" :value "Report"} "Report Match Result " [:span.glyphicon.glyphicon-ok]]]]])))

(defn- render-match [{:keys [matchdate team1 team2 reported-by] :as match}
                     players
                     & {:keys [admin] :or {admin false}}]
  (let [[t1p1 t1p2 t1score] (map team1 [:player1 :player2 :score])
        [t2p1 t2p2 t2score] (map team2 [:player1 :player2 :score])]
    [:tr
     [:td [:div.text-center (util/format-date matchdate)]]
     [:td [:div.text-center (util/render-team players [t1p1 t1p2])]]
     [:td [:div.text-center (util/format-score t1score)]]
     [:td [:div.text-center (util/render-team players [t2p1 t2p2])]]
     [:td [:div.text-center (util/format-score t2score)]]
     [:td [:div.text-center reported-by]]
     (when admin [:td [:button.btn.btn-danger {:type "submit"
                                               :name "matchid"
                                               :value (str (:match/id match))}
                       "Remove!"]])]))

(defn match-table-data [matches players & {:keys [admin] :or {admin false}}]
  [:table.table.table-hover.table-condensed [:caption [:h1 "Played Matches"]]
    [:thead
     [:tr
      [:th [:div.text-center "Date played"]]
      [:th [:div.text-center "Team 1"]]
      [:th [:div.text-center "Score"]]
      [:th [:div.text-center "Team 2"]]
      [:th [:div.text-center "Score"]]
      [:th [:div.text-center "Reported by"]]
      (when admin
        [:th ""])]]
    [:tbody
     (->> matches
          reverse
          (map (fn [match] (render-match match players :admin admin))))]])

(defn table [matches players]
  (html5
   (match-table-data matches players)))

(defn parse-form [p]
  {:matchdate       (-> p :matchdate   (util/parse-date :invalid-matchdate))
   :team1 {:player1 (-> p :team1player1 util/uuid-from-string)
           :player2 (-> p :team1player2 util/uuid-from-string)
           :score   (-> p :team1score   util/parse-id)}
   :team2 {:player1 (-> p :team2player1 util/uuid-from-string)
           :player2 (-> p :team2player2 util/uuid-from-string)
           :score   (-> p :team2score   util/parse-id)}})
