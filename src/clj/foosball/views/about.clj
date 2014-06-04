(ns foosball.views.about
  (:require [cfg.current :refer [project]]
            [foosball.software :as software]
            [hiccup.page :refer [html5]]))

(defn page []
  (html5
   [:div.col-lg-8.jumbotron
    [:h1 "Foosball"]
    [:p.lead (str "Version " (:version project)) ]
    [:p.lead "Copyright &copy; 2014 " [:a {:href "http://about.me/thomaschrstnsn"} "Thomas Christensen"]]
    [:h2 "Built using"]
    [:div.col-lg-12
     [:table.table.table-hover
      [:thead
       [:tr
        [:th.text-right "Software"]
        [:th "Version"]]]
      [:tbody
       (map (fn [{:keys [name url version]}]
              [:tr
               [:td.text-right [:a {:href url} name]]
               [:td version]])
            (software/software-dependencies project))]]]
    [:h2 "Styled using"]
    [:p.lead [:a {:href "http://getbootstrap.com"} "Bootstrap 3"] " with " [:a {:href "http://glyphicons.com"} "Glyphicons"] "."]]))
