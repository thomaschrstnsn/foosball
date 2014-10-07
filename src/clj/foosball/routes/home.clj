(ns foosball.routes.home
  (:require [compojure.core :as compojure :refer [GET]]
            [ring.util.response :as resp]))

(defn front-page [{:keys [config-options]}]
  (resp/resource-response (str "public/" (get config-options :root-page))))

(defn routes [deps]
  (compojure/routes
   (GET "/"      [] (front-page deps))))
