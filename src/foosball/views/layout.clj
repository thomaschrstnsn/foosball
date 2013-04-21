(ns foosball.views.layout
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]))

(defn header []
  [:div.navbar
   [:div.navbar-inner
    [:a.brand {:href "#"} "Foosball"]
    [:ul.nav
     [:li (link-to "/report/match"  "Report Match Result")]
     [:li (link-to "/matches"       "Played Matches")]
     [:li (link-to "/stats/players" "Player Statistics")]
     [:li (link-to "/stats/teams"   "Team Statistics")]
;     [:li.divider-vertical]
;     [:li (link-to "/about"         "About")]
     ]]])

(defn footer [] [:footer "Copyright &copy; " [:a {:href "mailto:thomas+foos@chrstnsn.dk"} "Thomas Christensen"]])

(defhtml
  base
  [& content]
  (html5
    [:head
     [:title "Foosball"]
     [:link {:rel "icon" :type "image/x-icon" :href "/favicon.ico"}]
     (include-css
       "/css/bootstrap.min.css"
       "/css/bootstrap-responsive.min.css"
       "/css/screen.css"
       ;"http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/css/jquery.dataTables.css"
       )
     (include-js
       "//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"
       "/js/bootstrap.min.js"
       ;"http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/jquery.dataTables.min.js"
       )]
    [:body content]))

(defn common [& content]
  (base (header) [:div.container content] (footer)))
