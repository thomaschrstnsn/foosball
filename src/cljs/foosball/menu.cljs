(ns foosball.menu
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [foosball.auth :as auth]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn classify-item [{:keys [id text route items seperator]}]
  (cond
   (and id text route) :location
   items               :sub-menu
   seperator           :seperator
   :else               nil))

(defmulti render-menu-item classify-item)

(defmethod render-menu-item :location [{:keys [id text active route]}]
  [:li (when active {:class "active"}) [:a {:href route} text]])

(defmethod render-menu-item :sub-menu [{:keys [text items]}]
  [:li.dropdown
   [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} text [:b.caret]]
   [:ul.dropdown-menu
    (map render-menu-item items)]])

(defmethod render-menu-item :seperator [_]
  [:li.divider])

(defn menu-item [item owner]
  (reify
    om/IRender
    (render [this]
      (html (render-menu-item item)))))

(defn auth-allowed-menu-locations [auth menu-locations]
  (filterv (fn [{:keys [login-required? admin-required?]}]
             (cond
              admin-required? (:admin? auth)
              login-required? (:logged-in? auth)
              :else true))
           menu-locations))

(defn menu-bar [app owner {:keys [menu-locations home-location]}]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [current-location auth]} app]
        (html [:div.navbar.navbar-static-top.navbar-default
               (let [{:keys [route text]} home-location]
                 [:a.navbar-brand {:href route} text])
               [:ul.nav.navbar-nav.pull-left
                (om/build-all menu-item
                              (auth-allowed-menu-locations auth menu-locations)
                              {:key :id
                               :fn (fn [{:keys [id] :as item}]
                                     (merge item {:active (or (= id current-location)
                                                              (isa? current-location id))}))})]
               (when auth
                 [:ul.nav.navbar-nav.pull-right
                  [:li (if (:logged-in? auth)
                         (auth/logout-form :extra-class "navbar-form"
                                           :text (str "Logout")
                                           :title (:username auth))
                         (auth/login-form  auth :form-class "navbar-form"))]])])))))
