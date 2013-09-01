(ns foosball.routes.admin
  (:use [compojure.core :only [defroutes context GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [ring.util.response :as response]
            [foosball.views.layout :as layout]
            [foosball.views.admin  :as admin]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [cemerick.friend :as friend]))

(defn admin-page []
  (layout/common "ADMIN" (admin/form (db/get-players) (db/get-matches))))

(defn rename-player [playerid newplayername]
  (info {:rename-player (util/symbols-as-map playerid newplayername)})
  (db/rename-player (util/parse-id playerid) newplayername)
  (response/redirect-after-post "/admin"))

(defn activate-player [id]
  (info {:activate-player id})
  (db/activate-player (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn deactivate-player [id]
  (info {:deactivate-player id})
  (db/deactivate-player (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defn remove-match [id]
  (info {:remove-match id})
  (db/delete-match (util/parse-id id))
  (response/redirect-after-post "/admin"))

(defroutes unprotected
              (GET  "/" [] (admin-page))
              (POST "/player/rename" [playerid newplayername] (rename-player playerid newplayername))
              (POST "/player/deactivate" [playerid] (deactivate-player playerid))
              (POST "/player/activate" [playerid] (activate-player playerid))
              (POST "/match/remove" [matchid] (remove-match matchid)))

(defroutes routes
  (context "/admin" request
           (friend/wrap-authorize unprotected #{auth/admin})))
