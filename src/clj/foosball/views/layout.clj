(ns foosball.views.layout
  (:require [cfg.current :refer [project]]
            [foosball.auth :as auth]
            [hiccup.element :refer [link-to]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn header []
  (let [a-matchup        (link-to "/matchup"       "Matchup")
        a-report         (link-to "/report/match"  "Report Match Result")
        a-stats-player   (link-to "/stats/players" "Player Statistics")
        a-stats-team     (link-to "/stats/teams"   "Team Statistics")
        a-player-log     (link-to "/player/log"    "Player Log")
        a-played-matches (link-to "/matches"       "Played Matches")
        a-admin          (link-to "/admin"         "Admin")
        a-about          (link-to "/about"         "About")]
    [:div.navbar.navbar-static-top.navbar-default
     [:a.navbar-brand {:href "/"} "Foosball"]
     [:ul.nav.navbar-nav.pull-left
      (when (auth/user?)
        (list [:li#nav-matchup a-matchup]
              [:li#nav-report a-report]))
      [:li.dropdown
       [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} "Statistics" [:b.caret]]
       [:ul.dropdown-menu
        [:li#nav-players-stats a-stats-player]
        [:li#nav-teams-stats   a-stats-team]
        [:li.divider]
        [:li#nav-player-log    a-player-log]]]
      [:li#nav-matches a-played-matches]
      (when (auth/admin?)
        [:li#nav-admin a-admin])
      [:li#nav-about {:title (str "Version " (:version project))} a-about]]
     [:ul.nav.navbar-nav.pull-right
      (let [playername (auth/current-auth :playername)]
        [:li (if-not (auth/current-auth)
               (auth/login-form  :form-class "navbar-form")
               (auth/logout-form :extra-class "navbar-form"
                                 :text (str "Logout")
                                 :title playername))])]]))

(defn footer [{:keys [cljs-repl-script-fn cljs-optimized?]}]
  (list
   (apply include-js ["/js/extern/jquery.min.js" "/js/extern/bootstrap.min.js"])
   (cljs-repl-script-fn)))

(defn base [config-options page-title & content]
  (html5
   [:head
    [:title (if page-title
              (str "Foosball - " page-title)
              "Foosball")]
    [:link {:rel "icon" :type "image/x-icon" :href "/favicon.ico"}]
    (include-css "/css/bootstrap.min.css")]
   [:body content]))

(defn common [config-options & {:keys [title content]}]
  (base config-options title (header) [:div.container content] (footer config-options)))
