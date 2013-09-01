(ns foosball.handler
  (:use [compojure.core :only [defroutes]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [noir.util.middleware    :as middleware]
            [compojure.route         :as route]
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

(def app (middleware/app-handler [home/routes
                                  report/routes
                                  stats/routes
                                  matchup/routes
                                  admin/routes
                                  user/routes
                                  app-routes]
                                 :middleware [auth/wrap-friend-openid]))

(def war-handler (middleware/war-handler app))
