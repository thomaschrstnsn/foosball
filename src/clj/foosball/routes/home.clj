(ns foosball.routes.home
  (:require [compojure.core :as compojure :refer [GET]]
            [ring.util.response :as resp]
            [foosball.models.domains :as d]
            [foosball.views.about :as about]
            [foosball.views.front :as front]
            [foosball.views.layout :as layout]))

(defn old-front-page [{:keys [db config-options]}]
  (layout/common config-options
                 :content (front/page (d/get-players db)
                                      (d/get-matches db))
                 :auto-refresh? true))

(defn about-page [{:keys [config-options]}]
  (layout/common config-options
                 :title "About" :content (about/page)))

(defn front-page [deps]
  (resp/resource-response "public/dev.html"))

(defn routes [deps]
  (compojure/routes
   (GET "/" [] (front-page deps))
   (GET "/old"   [] (old-front-page deps))
   (GET "/about" [] (about-page deps))))
