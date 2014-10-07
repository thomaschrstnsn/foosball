(ns foosball.app
  (:use [compojure.core :only [defroutes]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util              :as util]
            [foosball.routes.api        :as api]
            [foosball.routes.user       :as user]
            [foosball.routes.home       :as home]
            [foosball.auth              :as auth]
            [noir.util.middleware       :as middleware]
            [compojure.route            :as route]
            [com.stuartsierra.component :as component]
            [cfg.current :refer [project]]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defrecord App [ring-handler database config-options]
  component/Lifecycle
  (start [this]
    (info "Starting Foosball App")
    (let [route-fns    [api/routes
                        home/routes
                        user/routes]
          app-routes   (-> (mapv (fn [route-fn] (route-fn {:db database
                                                         :config-options config-options
                                                         :project project}))
                                 route-fns)
                           (conj app-routes))
          auth-wrapper (partial auth/wrap-friend-openid database)
          ring-handler (middleware/app-handler app-routes
                                               :middleware [auth-wrapper])]
      (assoc this :ring-handler ring-handler)))

  (stop [this]
    (info "Stopping Foosball App")
    (merge this {:app-handler nil :war-handler nil})))
