(ns foosball.main
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [chan <!]]
            [foosball.routes :as routes]
            [foosball.menu :as menu]
            [foosball.locations :as loc]
            [foosball.data :as data]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn app-root [app owner {:keys [menu] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      (data/go-update-data! "/api/auth" app :auth)
      {})

    om/IWillMount
    (will-mount [_]
      (let [req-location-chan (om/get-state owner :req-location-chan)]
        (go-loop []
          (let [[v c] (alts! [req-location-chan])]
            (condp = c
              req-location-chan (loc/request-new-location app v)
              nil)
            (recur)))))

    om/IRender
    (render [_]
      (html [:div (om/build menu/menu-bar app {:opts menu})
             [:div.container
              (loc/render-location app)]]))))

(debug "initializing application")
(let [app         (atom {})
      route-setup (routes/init!)]
  (om/root app-root app {:target     (. js/document (getElementById "app"))
                         :init-state {:req-location-chan (:req-location-chan route-setup)}
                         :opts       {:menu (select-keys route-setup [:home-location :menu-locations])}}))
