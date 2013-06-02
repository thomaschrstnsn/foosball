(ns foosball.routes.admin
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.admin  :as admin]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn admin-page []
  (layout/common (admin/form (db/get-players) (db/get-matches))))

(defn add-player [name]
  (info {:add-player name})
  (db/create-player name)
  (redirect-after-post "/admin"))

(defn remove-player [id]
  (info {:remove-player id})
  (db/delete-player (util/parse-id id))
  (redirect-after-post "/admin"))

(defn remove-match [id]
  (info {:remove-match id})
  (db/delete-match (util/parse-id id))
  (redirect-after-post "/admin"))

(defroutes admin-routes
  (GET "/admin" [] (admin-page))
  (POST "/admin/player/add" [playername] (add-player playername))
  (POST "/admin/player/remove" [playerid] (remove-player playerid))
  (POST "/admin/match/remove" [matchid] (remove-match matchid)))
