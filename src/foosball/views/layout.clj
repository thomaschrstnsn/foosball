(ns foosball.views.layout
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [cfg.current :only [project]])
  (:require [foosball.auth :as auth]
            [foosball.settings :as settings]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))

(defn header []
  [:div.navbar.navbar-static-top.navbar-default
   [:a.navbar-brand {:href "/"} "Foosball"]
   [:ul.nav.navbar-nav.pull-left
    (when (auth/user?) [:li (link-to "/matchup"       "Matchup")])
    (when (auth/user?) [:li (link-to "/report/match"  "Report Match Result")])
    [:li (link-to "/stats/players" "Player Statistics")]
    [:li (link-to "/stats/teams"   "Team Statistics")]
    [:li (link-to "/matches"       "Played Matches")]
    [:li (link-to "/player/log"    "Player Log")]
    (when (auth/admin?) [:li (link-to "/admin" "Admin")])
    [:li (link-to "/about"         "About")]]
   [:ul.nav.navbar-nav.pull-right
    (let [playername (auth/current-auth :playername)]
      [:li (if-not (auth/current-auth)
             (auth/login-form :form-class "navbar-form")
             (auth/logout-form :extra-class "navbar-form" :text (str "Logout"
                                                                     (when playername (str " " playername)))))])
    [:p.navbar-text "Version " (:version project)]]])

(defn footer [auto-refresh?]
  (list
   [:script {:type "text/javascript"} "goog.require(\"foosball.browser\");"]
   [:script {:type "text/javascript"} "foosball.browser.register_document_ready();"]
   (when auto-refresh?
     [:script {:type "text/javascript"} "foosball.browser.page_autorefresh(90)"])
   (when @cemerick.austin.repls/browser-repl-env
     [:script (cemerick.austin.repls/browser-connected-repl-js)])))

(defhtml base [page-title & content]
  (html5
    [:head
     [:title (if page-title
               (str "Foosball - " page-title)
               "Foosball")]
     [:link {:rel "icon" :type "image/x-icon" :href "/favicon.ico"}]
     (include-css "/css/bootstrap.min.css")
     (when-not settings/cljs-optimized?
       (include-js "/js/cljs/goog/base.js"))
     (include-js
       "/js/jquery.min.js"
       "/js/bootstrap.min.js"
       "/js/cljs/foosball.js")]
    [:body content]))

(defn common [& {:keys [title content auto-refresh?] :or {auto-refresh? false}}]
  (base title (header) [:div.container content] (footer auto-refresh?)))
