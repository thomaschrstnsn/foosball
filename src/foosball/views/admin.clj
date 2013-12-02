(ns foosball.views.admin
  (:require [foosball.views.match :refer [match-table-data]]
            [hiccup.page :refer [html5]]))

(defn- player-select [players]
  [:select.form-control {:name "playerid"}
   [:option "Player"]
   (->> players
        (map (fn [{:keys [id name]}] [:option {:value id} name])))])

(defn- player-select-form [action-uri header-label button-label class players]
  [:form.form-inline {:action action-uri :method "POST"}
   [:h1 header-label]
   [:div.form-group.col-lg-6 (player-select players)]
   [:div.form-group.col-lg-6
    [:button.btn {:class class :type "submit" :value "value"} button-label]]])

(defn form [players matches]
  (html5
   [:div
    [:form.form-horizontal {:action "/admin/player/rename" :method "POST"}
     [:h1 "Rename player"]
     [:div.form-group.col-lg-6 (player-select players)]
     [:div.form-group.col-lg-4 [:input.form-control {:type "text" :name "newplayername" :placeholder "New name"}]]
     [:div.form-group.col-lg-2 [:button.btn.btn-primary {:type "submit" :value "Rename"} "Rename!"]]]

    (player-select-form "/admin/player/activate" "Activate player" "Set active" "btn-success"
                        (filter (comp not :active) players))
    (player-select-form "/admin/player/deactivate" "Deactivate player" "Set inactive" "btn-danger"
                        (filter :active players))

    [:form {:action "/admin/match/remove" :method "POST"}
     (match-table-data matches players :admin true)]]))
