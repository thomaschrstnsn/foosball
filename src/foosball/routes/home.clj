(ns foosball.routes.home
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]
            [foosball.views.front  :as front]
            [foosball.models.db    :as db]
            [compojure.core :as compojure]))

(defn front-page [{:keys [db config-options]}]
  (layout/common config-options
                 :content (front/page (db/get-players-db db)
                                      (db/get-matches-db db))
                 :auto-refresh? true))

(defn about-page [{:keys [config-options]}]
  (layout/common config-options
                 :title "About" :content (about/page)))

(defn routes [deps]
  (compojure/routes
   (compojure/GET "/"      [] (front-page deps))
   (compojure/GET "/about" [] (about-page deps))))
