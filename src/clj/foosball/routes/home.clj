(ns foosball.routes.home
  (:require [compojure.core :as compojure :refer [GET]]
            [ring.util.response :as resp]))

(defn front-page [deps]
  (resp/resource-response "public/dev.html"))

(defn routes [deps]
  (compojure/routes
   (GET "/"      [] (front-page deps))))
