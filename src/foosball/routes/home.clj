(ns foosball.routes.home
  (:use compojure.core hiccup.element)
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.util :as util]))

(defn home-page []
  (layout/common
    (match/form)))

(defn about-page []
  (layout/common
   "this is the story of foosball... work in progress"))

(defn report-match [args]
  (println args)
  (home-page))

(defroutes home-routes
  (GET "/" [] (home-page))
  (POST "/" request (report-match request))
  (GET "/about" [] (about-page)))
