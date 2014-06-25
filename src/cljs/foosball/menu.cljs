(ns foosball.menu
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [foosball.console :refer-macros [debug]]))

(defn menu-item [{:keys [id text active route]} owner]
  (reify
    om/IRender
    (render [this]
      (html [:li (when active {:class "active"}) [:a {:href route} text]]))))

(defn login-form [url & {:keys [form-class button-class button-text] :or {button-text "Login"}}]
  (let [dom-id (str (gensym))]
    [:form {:class form-class :method "POST" :action "/login"}
     [:input {:type "hidden" :name "identifier" :value url :id dom-id}]
     [:input.button.btn.btn-info {:class button-class :type "submit" :value button-text}]]))

(defn logout-form [& {:keys [text extra-class title] :or {text "Logout"}}]
  [:form {:class extra-class :method "POST" :action "/logout"}
   [:input.button.btn.btn-default {:type "submit" :value text :title title}]])

(defn menu-bar [app owner {:keys [menu-locations home-location]}]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [current-location login]} app]
        (html [:div.navbar.navbar-static-top.navbar-default
               (let [{:keys [route text]} home-location]
                 [:a.navbar-brand {:href route} text])
               [:ul.nav.navbar-nav.pull-left
                (om/build-all menu-item
                              menu-locations
                              {:key :id
                               :fn (fn [{:keys [id] :as item}]
                                     (merge item {:active (or (= id current-location)
                                                              (isa? current-location id))}))})]
               (when login
                 [:ul.nav.navbar-nav.pull-right
                  [:li (if (:logged-in? login)
                         (logout-form :extra-class "navbar-form"
                                      :text (str "Logout")
                                      :title (:name login))
                         (login-form  (-> login :provider :url) :form-class "navbar-form"))]])])))))
