(ns foosball.views.layout
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]))

(defn header []
  [:div.navbar.navbar-fixed-top
   [:div.navbar-inner
    [:ul.nav
     [:li (link-to "/report/match" "Report Match Result")]
     [:li (link-to "/matches"      "Played Matches")]
;     [:li (link-to "/stats/team"   "Team Statistics")]
;     [:li (link-to "/stats/player" "Player Statistics")]
     ]]])

(defn footer [] [:footer "Copyright &copy; " [:a {:href "mailto:thomas+foos@chrstnsn.dk"} "Thomas Christensen"]])

(defhtml
  base
  [& content]
  (html5
    [:head
     [:title "Foosball"]
     (include-css
       "/css/bootstrap.min.css"
       "/css/bootstrap-responsive.min.css"
       "/css/screen.css")
     (include-js
       "//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"
       "/js/bootstrap.min.js")]
    [:body content]))

(defn common [& content]
  (base (header) [:div.container content] (footer)))
