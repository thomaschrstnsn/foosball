(ns foosball.views.layout
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [cfg.current :only [project]]))

(defn header []
  [:div.navbar.navbar-static-top
   [:a.navbar-brand {:href "/"} "Foosball"]
   [:ul.nav.navbar-nav.pull-left
    [:li (link-to "/stats/players" "Player Statistics")]
    [:li (link-to "/stats/teams"   "Team Statistics")]
    [:li (link-to "/matchup"       "Matchup")]
    [:li (link-to "/report/match"  "Report Match Result")]
    [:li (link-to "/matches"       "Played Matches")]
    [:li (link-to "/player/log"    "Player Log")]
    [:li (link-to "/about"         "About")]]
   [:ul.nav.navbar-nav.pull-right
     [:p.navbar-text "Version " (:version project)]]])

(defn footer []
  [:script "foosball.browser.register_document_ready()"])

(defhtml base [& content]
  (html5
    [:head
     [:title "Foosball"]
     [:link {:rel "icon" :type "image/x-icon" :href "/favicon.ico"}]
     (include-css "/css/bootstrap.min.css")
     (include-js
       "//ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"
       "/js/bootstrap.min.js"
       "/js/foosball.js")]
    [:body content]))

(defn common [& content]
  (base (header) [:div.container content] (footer)))
