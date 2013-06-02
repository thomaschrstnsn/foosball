(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]))

(defn about-page []
  (layout/common (about/page)))

(defroutes home-routes
  (GET "/about" [] (about-page)))
