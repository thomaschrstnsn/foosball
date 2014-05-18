(ns foosball.main
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [foosball.routes :as routes]
            [foosball.menu :as menu]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(log "hello world from ClojureScript")

(defmulti request-new-location (fn [app req] (-> @app :current-location)))

(defmulti handle-new-location (fn [app req] (:id req)))

(defmethod request-new-location :default [app new]
  (handle-new-location app new))

(defn set-location [app id]
  (debug @app id)
  (om/update! app :current-location id))

(defmethod handle-new-location :default [app {:keys [id]}]
  (set-location app id))

(defn app-root [app owner {:keys [menu] :as opts}]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [req-location-chan (om/get-state owner :req-location-chan)]
        (go-loop []
          (let [[v c] (alts! [req-location-chan])]
            (condp = c
              req-location-chan (request-new-location app v)
              nil)
            (recur)))))

    om/IRender
    (render [_]
      (html [:div (om/build menu/menu-bar app {:opts menu})
             [:div.container
              (let [location (:current-location app)]
                (list
                 [:h1 (str location)]
                 [:p "Med dig"]))]]))))

(let [app (atom {})
      route-setup (routes/init!)]
  (om/root app-root app {:target     (. js/document (getElementById "app"))
                         :init-state {:req-location-chan (:req-location-chan route-setup)}
                         :opts       {:menu (select-keys route-setup [:home-location :menu-locations])}}))
