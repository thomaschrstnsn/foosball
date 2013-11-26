(ns foosball.routes.home
  (:require [foosball.views.layout :as layout]
            [foosball.views.about  :as about]
            [foosball.views.front  :as front]
            [foosball.models.db    :as db]
            [compojure.core :as compojure]))

(defn front-page [db]
  (layout/common :content (front/page (db/get-players-db db) (db/get-matches-db db))
                 :auto-refresh? true))

(defn about-page []
  (layout/common :title "About" :content (about/page)))

(defn routes [database]
  (compojure/routes
   (compojure/GET "/"      [] (front-page database))
   (compojure/GET "/about" [] (about-page))))
