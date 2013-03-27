(ns foosball.views.player
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [foosball.util :only [format-time]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn form [players]
  (html5
   [:form {:action "/player/add" :method "POST"}
    [:div.input-append
     [:input.span2 {:type "text" :name "playername" :placeholder "New Player"}]
     [:button.btn.btn-primary {:type "submit" :value "Report"} "Add!"]]]

   [:form {:action "/player/remove" :method "POST"}
    [:div.input-append
     [:select {:name "playerid"}
      (->> players
           (map (fn [{:keys [id name]}] [:option {:value id} name])))]
     [:button.btn.btn-danger {:type "submit" :value "delete"} "Delete!"]]]))
