(ns foosball.routes.home
  (:require [compojure.core :as compojure :refer [GET]]
            [foosball.models.domains :as d]
            [foosball.views.about :as about]
            [foosball.views.front :as front]
            [foosball.views.layout :as layout]))

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
