(ns foosball.menu
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn menu-item [{:keys [id text active route]} owner]
  (reify
    om/IRender
    (render [this]
      (html [:li (when active {:class "active"}) [:a {:href route} text]]))))

(defn menu-bar [app owner {:keys [menu-locations home-location]}]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [current-location]} app]
        (html [:div.navbar.navbar-static-top.navbar-default
               (let [{:keys [route text]} home-location]
                 [:a.navbar-brand {:href route} text])
               [:ul.nav.navbar-nav.pull-left
                (om/build-all menu-item
                              menu-locations
                              {:key :id
                               :fn (fn [{:keys [id] :as item}]
                                     (merge item {:active (or (= id current-location)
                                                              (isa? current-location id))}))})]])))))
