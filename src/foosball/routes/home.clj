(ns foosball.routes.home
  (:use [compojure.core :only [defroutes GET]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]
            [foosball.views.front  :as front]
            [foosball.models.db    :as db]))

(defn front-page []
  (layout/common :content (front/page (db/get-players) (db/get-matches))
                 :auto-refresh? true))

(defn about-page []
  (layout/common :title "About" :content (about/page)))

(defroutes routes
  (GET "/"      [] (front-page))
  (GET "/about" [] (about-page)))
