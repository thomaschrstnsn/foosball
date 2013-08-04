(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]
            [foosball.views.front  :as front]
            [foosball.models.db    :as db]))

(defn front-page []
  (layout/common (front/page (db/get-players) (db/get-matches))))

(defn about-page []
  (layout/common (about/page)))

(defroutes home-routes
  (GET "/"      [] (front-page))
  (GET "/about" [] (about-page)))
