(ns foosball.routes.admin
  (:use [compojure.core :only [GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [ring.util.response :as response]
            [foosball.views.layout :as layout]
            [foosball.views.admin  :as admin]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.domains :as d]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(defn admin-page [{:keys [db config-options]}]
  (layout/common config-options
                 :title "ADMIN" :content (admin/form (d/get-players db)
                                                     (d/get-matches db))))

(defn rename-player [{:keys [db]} playerid newplayername]
  (info {:rename-player (util/symbols-as-map playerid newplayername)})
  (d/rename-player! db (util/parse-id playerid) newplayername)
  (response/redirect-after-post "/admin"))

(defn activate-player [{:keys [db]} id]
  (info {:activate-player id})
  (d/activate-player! db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn deactivate-player [{:keys [db]} id]
  (info {:deactivate-player id})
  (d/deactivate-player! db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn remove-match [{:keys [db]} id]
  (info {:remove-match id})
  (d/delete-match! db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn routes [deps]
  (let [admin-routes (compojure/routes
                      (GET  "/" []
                            (admin-page deps))
                      (POST "/player/rename" [playerid newplayername]
                            (rename-player deps playerid newplayername))
                      (POST "/player/deactivate" [playerid]
                            (deactivate-player deps playerid))
                      (POST "/player/activate" [playerid]
                            (activate-player deps playerid))
                      (POST "/match/remove" [matchid]
                            (remove-match deps matchid)))]
    (compojure/context "/admin" request
                       (friend/wrap-authorize admin-routes #{auth/admin}))))
