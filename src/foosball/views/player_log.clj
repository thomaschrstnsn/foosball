(ns foosball.views.player-log
  (:use [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [foosball.statistics team-player ratings])
  (:use [foosball.util]))

(defn- player-option [selected {:keys [id name]}]
  [:option (merge {:value id}
                  (when (= id selected) {:selected "selected"}))
   name])

(defn- players-select [id players & [selected]]
  (let [grouped  (group-by :active players)
        active   (get grouped true)
        inactive (get grouped false)]
    [:select.input-medium.submit-on-select {:id id :name id}
     [:option {:value "nil" :disabled "disabled" :selected "selected"} "Active players"]
     (map (partial player-option selected) active)
     (when inactive
       [:option {:value "nil" :disabled "disabled"} "Inactive players"])
     (when inactive
       (map (partial player-option selected) inactive))]))

(defn- render-log [players l]
  [:tr
   [:td (format-datetime (:matchdate l))]
   [:td (->> (:team-mate l) (get-player-by-name players) link-to-player-log)]
   [:td (->> (:opponents l)
             (map #(->> % (get-player-by-name players) link-to-player-log))
             (interpose ", "))]
   [:td (format-value (* 100 (:expected l)) :printer format-percentage :class? (partial not= (double 50)) :checker (partial < 50))]
   [:td (format-value (:win? l) :class? nil :printer {true "Won" false "Lost"} :checker true?)]
   [:td (format-value (:delta l) :printer format-rating)]
   [:td (format-value (:new-rating l) :printer format-rating :class? nil :checker (partial < 1500))]])

(defn player-table [matches players {:keys [name active] :as player}]
  [:table.table.table-hover
   [:caption [:h2 (str "Played Matches: " name (when-not active " (inactive)")) ]]
   [:thead [:tr
            [:th "Match date"]
            [:th "Team mate"]
            [:th "Opponents"]
            [:th "Expected"]
            [:th "Actual"]
            [:th "Diff rating"]
            [:th "New Rating"]]
    [:tbody
     (->> matches
          ratings-with-log
          :logs
          (filter (comp (partial = name) :player))
          reverse
          (map (partial render-log players)))]]])

(defn player-log-page [matches players selected-playerid]
  (let [playerid (parse-id selected-playerid)
        player   (->> players (filter (fn [p] (= (:id p) playerid))) first)]
    (html5
     (auto-refresh-page)
     [:h1 (str "Player Log")]
     [:form {:action "/player/log" :method "GET"}
      [:div.input-append
       (players-select "playerid" players playerid)
       [:button.btn {:type "submit" :value "select"} "Select"]]]
     (when player (player-table matches players player)))))
