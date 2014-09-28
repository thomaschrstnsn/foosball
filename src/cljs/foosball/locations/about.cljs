(ns foosball.locations.about
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]))

(defn handle [app v]
  (om/update! app :software-dependencies nil)
  (data/go-get-data! {:server-url "/api/about/software"
                      :app  app
                      :key  :software-dependencies})
  (loc/set-location app (:id v)))

(defn render [{:keys [software-dependencies version]}]
  [:div.col-lg-8.jumbotron
   [:h1 "Foosball"]
   (when version
     [:p.lead (str "Version " version)])
    [:p.lead "Copyright © 2014 " [:a {:href "http://about.me/thomaschrstnsn"} "Thomas Christensen"]]
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
            software-dependencies)]]]
    [:h2 "Styled using"]
    [:p.lead [:a {:href "http://getbootstrap.com"} "Bootstrap 3"] " with " [:a {:href "http://glyphicons.com"} "Glyphicons"] "."]])
