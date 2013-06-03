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
    "https://github.com/ptaoussanis/timbre"]
   ['midje/midje
    "Midje"
    "https://github.com/marick/Midje"]])

(defn page []
  (html5
   [:p.lead "Copyright &copy; 2013 " [:a {:href "mailto:thomas+foos@chrstnsn.dk"} "Thomas Christensen"]]
   [:table.table.table-hover.table-bordered.span4 [:caption [:h1 "Built using"]]
    [:thead
     [:tr
      [:th "Software"]
      [:th "Version"]]]
    [:tbody
     (map (fn [[k name url]]
            [:tr
             [:td [:a {:href url} name]]
             [:td (k (current-versions))]])
          softwares)]]))
