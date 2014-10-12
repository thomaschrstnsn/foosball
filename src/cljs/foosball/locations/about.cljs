(ns foosball.locations.about
  (:require [om.core :as om :include-macros true]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.spinners :refer [spinner]]))

(defn handle [app v]
  (om/update! app :software-dependencies nil)
  (data/go-get-data! {:server-url "/api/about/software"
                      :app  app
                      :key  :software-dependencies
                      :satisfied-with-existing-app-data? true})
  (data/go-get-data! {:server-url "/api/about/version"
                      :app  app
                      :key  :version
                      :satisfied-with-existing-app-data? true
                      :server-data-transform :version})
  (loc/set-location app (:id v)))

(defn render [{:keys [software-dependencies version]}]
  (if-not (and software-dependencies version)
    (spinner)
    [:div.col-lg-8.jumbotron
     [:a {:href "https://github.com/thomaschrstnsn/foosball"}
      [:img {:style {:position "absolute"
                     :top 0
                     :right 0
                     :border 0}
             :src "https://camo.githubusercontent.com/652c5b9acfaddf3a9c326fa6bde407b87f7be0f4/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6f72616e67655f6666373630302e706e67"
             :alt "Fork me on Github"
             :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_orange_ff7600.png"}]]
     [:h1 "Foosball"]
     (let [changelog-ref (str "https://github.com/thomaschrstnsn/foosball/"
                              "blob/master/CHANGELOG.org"
                              (f/changelog-anchor-for-version version))]
       [:p.lead "Version " [:a {:href changelog-ref} (str version)]])
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
     [:p.lead [:a {:href "http://getbootstrap.com"} "Bootstrap 3"] " with " [:a {:href "http://glyphicons.com"} "Glyphicons"] "."]]))
