(ns foosball.routes.stats
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]
        [compojure.core :only [GET POST]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.stats  :as stats]
            [foosball.views.player-log :as player-log]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [compojure.core :as compojure]))

(defn stats-players [{:keys [config-options db]} sort order]
  (layout/common config-options
                 :title "Player Statistics"
                 :auto-refresh? true
                 :content (stats/player-table (db/get-matches-db db)
                                              (db/get-players-db db)
                                              :sort (keyword sort)
                                              :order (keyword order))))

(defn stats-teams [{:keys [db config-options]} sort order]
  (layout/common config-options
                 :title "Team Statistics"
                 :auto-refresh? true
                 :content (stats/team-table (db/get-matches-db db)
                                            (db/get-players-db db)
                                            :sort (keyword sort)
                                            :order (keyword order))))

(defn log-for-player [{:keys [config-options db]} playerid]
  (layout/common config-options
                 :title "Player Log"
                 :auto-refresh? true
                 :content (player-log/player-log-page (db/get-matches-db db)
                                                      (db/get-players-db db)
                                                      playerid)))

(defn routes [deps]
  (compojure/routes
   (GET "/stats/players" [sort order] (stats-players  deps sort order))
   (GET "/stats/teams"   [sort order] (stats-teams    deps sort order))
   (GET "/player/log"    [playerid]   (log-for-player deps playerid))))
