(ns foosball.views.admin
  (:use hiccup.form
        [foosball.views.match :only [match-table-data]]
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [foosball.util :only [format-time]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn form [players matches]
  (html5
   [:form {:action "/admin/player/add" :method "POST"}
    [:div.input-append
     [:h1 "Add player"]
     [:input.span3 {:type "text" :name "playername" :placeholder "New Player"}]
     [:button.btn.btn-primary {:type "submit" :value "Report"} "Add!"]]]

   [:form {:action "/admin/player/remove" :method "POST"}
    [:div.input-append
     [:h1 "Remove player"]
     [:select.span3 {:name "playerid"}
      [:option "Player"]
      (->> players
           (map (fn [{:keys [id name]}] [:option {:value id} name])))]
     [:button.btn.btn-danger {:type "submit" :value "delete"} "Remove!"]]]

   [:form {:action "/admin/match/remove" :method "POST"}
    (match-table-data matches :admin true)]))
