(ns foosball.views.layout
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [cfg.current :only [project]]))

(defn header []
  [:div.navbar
   [:div.navbar-inner
    [:a.brand {:href "/"} "Foosball"]
    [:ul.nav
     [:li (link-to "/report/match"  "Report Match Result")]
     [:li (link-to "/matches"       "Played Matches")]
     [:li (link-to "/stats/players" "Player Statistics")]
     [:li (link-to "/stats/teams"   "Team Statistics")]
     [:li.divider-vertical]
     [:li (link-to "/about"         "About")]]]])

(defn footer []
  [:footer
   [:script "$(document).ready(foosball.main.page_loaded)"]
   [:div.row-fluid
    [:div.span4.offset4.text-center
     "Copyright &copy; " [:a {:href "mailto:thomas+foos@chrstnsn.dk"} "Thomas Christensen"]]
    [:div.span4.text-right
     [:small (str "Version " (:version project))]]]])

(defhtml base [& content]
  (html5
    [:head
     [:title "Foosball"]
     [:link {:rel "icon" :type "image/x-icon" :href "/favicon.ico"}]
     (include-css
       "/css/bootstrap.min.css"
       "/css/bootstrap-responsive.min.css"
       "/css/screen.css")
     (include-js
       "//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"
       "/js/bootstrap.min.js"
       "/js/foosball.js")]
    [:body content]))

(defn common [& content]
  (base (header) [:div.container content] (footer)))
