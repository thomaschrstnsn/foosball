(ns foosball.app
  (:use [compojure.core :only [defroutes]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util              :as util]
            [foosball.routes.home       :as home]
            [foosball.routes.report     :as report]
            [foosball.routes.stats      :as stats]
            [foosball.routes.admin      :as admin]
            [foosball.routes.matchup    :as matchup]
            [foosball.routes.user       :as user]
            [foosball.auth              :as auth]
            [noir.util.middleware       :as middleware]
            [compojure.route            :as route]
            [com.stuartsierra.component :as component]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defrecord App [ring-handler database config-options]
  component/Lifecycle

  (start [this]
    (info "Starting Foosball App")
    (let [route-fns    [admin/routes
                        home/routes
                        matchup/routes
                        report/routes
                        stats/routes
                        user/routes]
          app-routes   (-> (map (fn [route-fn] (route-fn {:db database
                                                         :config-options config-options}))
                                route-fns)
                           (concat [app-routes])
                           (vec))
          auth-wrapper (partial auth/wrap-friend-openid database)
          ring-handler (middleware/app-handler app-routes
                                               :middleware [auth-wrapper])]
      (assoc this :ring-handler ring-handler)))

  (stop [this]
    (info "Stopping Foosball App")
    (merge this {:app-handler nil :war-handler nil})))
