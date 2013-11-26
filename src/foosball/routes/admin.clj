(ns foosball.routes.admin
  (:use [compojure.core :only [GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [ring.util.response :as response]
            [foosball.views.layout :as layout]
            [foosball.views.admin  :as admin]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(defn admin-page [database]
  (layout/common :title "ADMIN" :content (admin/form (db/get-players-db database)
                                                     (db/get-matches-db database))))

(defn rename-player [db playerid newplayername]
  (info {:rename-player (util/symbols-as-map playerid newplayername)})
  (db/rename-player-db db (util/parse-id playerid) newplayername)
  (response/redirect-after-post "/admin"))

(defn activate-player [db id]
  (info {:activate-player id})
  (db/activate-player-db db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn deactivate-player [db id]
  (info {:deactivate-player id})
  (db/deactivate-player-db db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn remove-match [db id]
  (info {:remove-match id})
  (db/delete-match-db db (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn routes [db]
  (let [admin-routes (compojure/routes
                      (GET  "/" []
                            (admin-page db))
                      (POST "/player/rename" [playerid newplayername]
                            (rename-player db playerid newplayername))
                      (POST "/player/deactivate" [playerid]
                            (deactivate-player db playerid))
                      (POST "/player/activate" [playerid]
                            (activate-player db playerid))
                      (POST "/match/remove" [matchid]
                            (remove-match db matchid)))]
    (compojure/context "/admin" request
             (friend/wrap-authorize admin-routes #{auth/admin}))))
