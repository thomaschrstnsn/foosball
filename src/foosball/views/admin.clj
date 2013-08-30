(ns foosball.views.admin
  (:use hiccup.form
        [foosball.views.match :only [match-table-data]]
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- player-select [action-uri header-label button-label class players]
  [:form.form-inline {:action action-uri :method "POST"}
   [:h1 header-label]
   [:div.form-group.col-lg-6
     [:select.form-control {:name "playerid"}
      [:option "Player"]
      (->> players
           (map (fn [{:keys [id name]}] [:option {:value id} name])))]]
   [:div.form-group.col-lg-6
    [:button.btn {:class class :type "submit" :value "value"} button-label]]])

(defn form [players matches]
  (html5
   [:div
    [:form.form-inline {:action "/admin/player/add" :method "POST"}
     [:h1 "Add player"]
     [:div.form-group.col-lg-6
      [:input.form-control.col-lg-3 {:type "text" :name "playername" :placeholder "New Player"}]]
     [:div.form-group.col-lg-6
      [:button.btn.btn-primary {:type "submit" :value "Report"} "Add!"]]]

    (player-select "/admin/player/activate" "Activate player" "Set active" "btn-success"
                   (filter (comp not :active) players))
    (player-select "/admin/player/deactivate" "Deactivate player" "Set inactive" "btn-danger"
                   (filter :active players))

    [:form {:action "/admin/match/remove" :method "POST"}
     (match-table-data matches players :admin true)]]))
