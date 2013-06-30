(ns foosball.handler
  (:use foosball.routes.home
        foosball.routes.report
        foosball.routes.admin
        foosball.routes.stats
        foosball.routes.matchup
        compojure.core)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [noir.util.middleware :as middleware]
            [compojure.route :as route]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def all-routes [home-routes
                 report-routes
                 stats-routes
                 matchup-routes
                 admin-routes
                 app-routes])

(def app (-> all-routes
             middleware/app-handler))

(def war-handler (middleware/war-handler app))
