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
   ['compojure/compojure
    "Compojure"
    "https://github.com/weavejester/compojure"]
   ['lib-noir/lib-noir
    "lib-noir"
    "https://github.com/noir-clojure/lib-noir"]
   ['hiccup/hiccup
    "Hiccup"
    "https://github.com/weavejester/hiccup"]
   ['prismatic/dommy
    "Dommy"
    "https://github.com/Prismatic/dommy"]
   ['com.taoensso/timbre
    "Timbre"
    "https://github.com/ptaoussanis/timbre"]])

(defn page []
  (html5
   [:div.col-lg-8.jumbotron
    [:h1 "Foosball"]
    [:p.lead "Copyright &copy; 2013 " [:a {:href "mailto:thomas+foos@chrstnsn.dk"} "Thomas Christensen"]]
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
            softwares)]]]]))
