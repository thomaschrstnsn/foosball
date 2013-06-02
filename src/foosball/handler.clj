(ns foosball.handler
  (:use foosball.routes.home foosball.routes.report foosball.routes.admin foosball.routes.stats compojure.core)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [foosball.models.schema :as schema]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (info "initializing database")
  (schema/initialize)
  (info "foosball started successfully..."))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (info "shutting down..."))

(def all-routes [home-routes
                 report-routes
                 stats-routes
                 admin-routes
                 app-routes])

(def app (-> all-routes
             middleware/app-handler
             ;;add your middlewares here
             ))

(def war-handler (middleware/war-handler app))
