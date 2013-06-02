(ns foosball.routes.stats
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.stats  :as stats]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn stats-players [sort order]
  (layout/common (stats/player-table (db/get-matches) :sort (keyword sort) :order (keyword order))))

(defn stats-teams [sort order]
  (layout/common (stats/team-table (db/get-matches) :sort (keyword sort) :order (keyword order))))

(defroutes stats-routes
  (GET "/stats/players" [sort order] (stats-players sort order))
  (GET "/stats/teams"   [sort order] (stats-teams sort order)))
