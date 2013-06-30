(ns foosball.views.admin
  (:use hiccup.form
        [foosball.views.match :only [match-table-data]]
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- player-select [action-uri header-label button-label class players]
  [:form {:action action-uri :method "POST"}
    [:div.input-append
     [:h1 header-label]
     [:select.span3 {:name "playerid"}
      [:option "Player"]
      (->> players
           (map (fn [{:keys [id name]}] [:option {:value id} name])))]
     [:button.btn {:class class :type "submit" :value "value"} button-label]]])

(defn form [players matches]
  (html5
   [:form {:action "/admin/player/add" :method "POST"}
    [:div.input-append
     [:h1 "Add player"]
     [:input.span3 {:type "text" :name "playername" :placeholder "New Player"}]
     [:button.btn.btn-primary {:type "submit" :value "Report"} "Add!"]]]

   (player-select "/admin/player/activate" "Activate player" "Set active" "btn-success"
                  (filter (comp not :active) players))

   (player-select "/admin/player/deactivate" "Deactivate player" "Set inactive" "btn-danger"
                  (filter :active players))

   [:form {:action "/admin/match/remove" :method "POST"}
    (match-table-data matches players :admin true)]))
