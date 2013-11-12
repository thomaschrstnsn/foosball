(ns foosball.views.about
  (:use hiccup.form
        [hiccup.def :only [defhtml]]
        [hiccup.element :only [link-to]]
        [hiccup.page :only [html5 include-js include-css]]
        [cfg.current :only [project]]
        [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- current-versions []
  (->>  (:dependencies project) (map (partial take 2)) flatten (apply assoc {})))

(def softwares
  [['org.clojure/clojure
    "Clojure"
    "http://clojure.org"]
   ['com.datomic/datomic-free
    "Datomic"
    "http://datomic.com"]
   ['ring-server/ring-server
    "Ring"
    "https://github.com/ring-clojure/ring"]
   ['com.cemerick/friend
    "Friend"
    "https://github.com/cemerick/friend"]
   ['compojure/compojure
    "Compojure"
    "https://github.com/weavejester/compojure"]
   ['lib-noir/lib-noir
    "lib-noir"
    "https://github.com/noir-clojure/lib-noir"]
   ['hiccup/hiccup
    "Hiccup"
    "https://github.com/weavejester/hiccup"]
   ['clj-time/clj-time
    "clj-time"
    "https://github.com/clj-time/clj-time"]
   ['com.taoensso/timbre
    "Timbre"
    "https://github.com/ptaoussanis/timbre"]])

(defn page []
  (html5
   [:div.col-lg-8.jumbotron
    [:h1 "Foosball"]
    [:p.lead (str "Version " (:version project)) ]
    [:p.lead "Copyright &copy; 2013 " [:a {:href "http://about.me/thomaschrstnsn"} "Thomas Christensen"]]
    [:h2 "Built using"]
    [:div.col-lg-12
     [:table.table.table-hover
      [:thead
       [:tr
        [:th.text-right "Software"]
        [:th "Version"]]]
      [:tbody
       (map (fn [[k name url]]
              [:tr
               [:td.text-right [:a {:href url} name]]
               [:td (k (current-versions))]])
            softwares)]]]
    [:h2 "Styled using"]
    [:p.lead [:a {:href "http://getbootstrap.com"} "Bootstrap 3"] " with " [:a {:href "http://glyphicons.com"} "Glyphicons"] "."]]))
