(ns foosball.handler
  (:use [compojure.core :only [defroutes]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [noir.util.middleware    :as middleware]
            [compojure.route         :as route]
            [com.stuartsierra.component :as component]
            [foosball.util           :as util]
            [foosball.routes.home    :as home]
            [foosball.routes.report  :as report]
            [foosball.routes.stats   :as stats]
            [foosball.routes.admin   :as admin]
            [foosball.routes.matchup :as matchup]
            [foosball.routes.user    :as user]
            [foosball.auth           :as auth]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defrecord App [app-handler war-handler database]
  component/Lifecycle

  (start [this]
    (info "Starting Foosball App")
    (let [route-fns   [admin/routes
                       home/routes
                       matchup/routes
                       report/routes
                       stats/routes
                       user/routes]
          app-routes  (-> (map (fn [route-fn] (route-fn database)) route-fns)
                          (concat [app-routes])
                          (vec))
          app-handler (middleware/app-handler app-routes
                                              :middleware [auth/wrap-friend-openid])
          war-handler (middleware/war-handler app-handler)]
      (merge this {:app-handler app-handler :war-handler war-handler})))

  (stop [this]
    (info "Stopping Foosball App")
    (merge this {:app-handler nil :war-handler nil})))
