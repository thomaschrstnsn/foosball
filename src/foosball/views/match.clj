(ns
  foosball.views.match
  (:use
    hiccup.form
    [hiccup.def :only [defhtml]]
    [hiccup.element :only [link-to]]
    [hiccup.page :only [html5 include-js include-css]]
    [foosball.util :only [format-time]]))

(defn- team-controls [team-num]
  (let [idp1    (str "team" team-num "player" 1)
        idp2    (str "team" team-num "player" 2)
        idscore (str "team" team-num "score")]
    [:div.control-group
     [:h3 (str "Team " team-num)]
     [:label.control-label {:for idp1} (str "Player" 1)]
     [:div.controls [:input.input-small  {:id idp1 :name idp1 :type "text" :placeholder "Player name"}]]

     [:label.control-label {:for idp2} (str "Player" 2)]
     [:div.controls [:input.input-small  {:id idp2 :name idp2 :type "text" :placeholder "Player name"}]]

     [:label.control-label {:for idscore} "Score"]
     [:div.controls [:input.input-small {:id idscore :name idscore :type "number" :placeholder "0" :min "0" :max "11"}]]]))

(defn form []
  (html5
   [:form.form-horizontal {:action "/match" :method "POST"}
    (team-controls 1)
    (team-controls 2)

    [:div.control-group
     [:div.controls [:input.input-small {:id "matchdate" :name "matchdate" :type "date" :value (format-time (new java.util.Date))}]]]

    [:div.control-group
     [:div.controls [:button.btn.btn-primary {:type "submit" :value "Report"} "Report"]]] ]))
