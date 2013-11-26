(ns foosball.routes.stats
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.stats  :as stats]
            [foosball.views.player-log :as player-log]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [compojure.core :as compojure]))

(defn stats-players [database sort order]
  (layout/common :title "Player Statistics"
                 :auto-refresh? true
                 :content (stats/player-table (db/get-matches-db database)
                                              (db/get-players-db database)
                                              :sort (keyword sort)
                                              :order (keyword order))))

(defn stats-teams [database sort order]
  (layout/common :title "Team Statistics"
                 :auto-refresh? true
                 :content (stats/team-table (db/get-matches-db database)
                                            (db/get-players-db database)
                                            :sort (keyword sort)
                                            :order (keyword order))))

(defn log-for-player [database playerid]
  (layout/common :title "Player Log"
                 :auto-refresh? true
                 :content (player-log/player-log-page (db/get-matches-db database)
                                                      (db/get-players-db database)
                                                      playerid)))

(defn routes [database]
  (compojure/routes
   (compojure/GET "/stats/players" [sort order] (stats-players  database sort order))
   (compojure/GET "/stats/teams"   [sort order] (stats-teams    database sort order))
   (compojure/GET "/player/log"    [playerid]   (log-for-player database playerid))))
