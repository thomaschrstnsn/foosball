(ns foosball.routes.home
  (:use [compojure.core :only [GET]])
  (:require [foosball.views.layout   :as layout]
            [foosball.views.about    :as about]
            [foosball.views.front    :as front]
            [foosball.models.domains :as d]
            [compojure.core :as compojure]))

(defn front-page [{:keys [db config-options]}]
  (layout/common config-options
                 :content (front/page (d/get-players db)
                                      (d/get-matches db))
                 :auto-refresh? true))

(defn about-page [{:keys [config-options]}]
  (layout/common config-options
                 :title "About" :content (about/page)))

(defn routes [deps]
  (compojure/routes
   (GET "/"      [] (front-page deps))
   (GET "/about" [] (about-page deps))))
