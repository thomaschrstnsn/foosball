(ns foosball.routes.home
  (:use compojure.core hiccup.element)
  (:require [foosball.views.layout :as layout]
            [foosball.util :as util]))

(defn home-page []
  (layout/common
    (util/md->html "/md/docs.md")))

(defn about-page []
  (layout/common
   "this is the story of foosball... work in progress"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page)))