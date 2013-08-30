(ns foosball.routes.stats
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.stats  :as stats]
            [foosball.views.player-log :as player-log]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn stats-players [sort order]
  (layout/common "Player Statistics" (stats/player-table (db/get-matches) (db/get-players) :sort (keyword sort) :order (keyword order))))

(defn stats-teams [sort order]
  (layout/common "Team Statistics" (stats/team-table (db/get-matches) (db/get-players) :sort (keyword sort) :order (keyword order))))

(defn log-for-player [playerid]
  (layout/common "Player Log" (player-log/player-log-page (db/get-matches) (db/get-players) playerid)))

(defroutes stats-routes
  (GET "/stats/players" [sort order] (stats-players sort order))
  (GET "/stats/teams"   [sort order] (stats-teams sort order))
  (GET "/player/log"    [playerid]   (log-for-player playerid)))
