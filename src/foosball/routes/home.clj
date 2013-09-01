(ns foosball.routes.home
  (:use [compojure.core :only [defroutes GET]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]
            [foosball.views.front  :as front]
            [foosball.models.db    :as db]))

(defn front-page []
  (layout/common nil (front/page (db/get-players) (db/get-matches))))

(defn about-page []
  (layout/common "About" (about/page)))

(defroutes routes
  (GET "/"      [] (front-page))
  (GET "/about" [] (about-page)))
