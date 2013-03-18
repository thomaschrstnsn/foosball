(ns
  foosball.views.player
  (:use
    hiccup.form
    [hiccup.def :only [defhtml]]
    [hiccup.element :only [link-to]]
    [hiccup.page :only [html5 include-js include-css]]
    [foosball.util :only [format-time]]))

(defn form []
  (html5
   [:form {:action "/newplayer" :method "POST"}

    [:div.input-append
     [:input.span2 {:type "text" :name "playername" :placeholder "New Player"}]
     [:button.btn.btn-primary {:type "submit" :value "Report"} "Add!"]
     ]]))
