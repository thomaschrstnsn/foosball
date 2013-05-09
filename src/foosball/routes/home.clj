(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.views.stats  :as stats]
            [foosball.views.admin  :as admin]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn report-match-page []
  (layout/common (match/form (db/get-players))))

(defn report-match [{:keys [params]}]
  (info {:report-match-params params})
  (let [validated-report (->> params
                              match/parse-form
                              match/validate-report)]
    (if (->> validated-report :validation-errors empty?)
      (do
        (info {:validated-report validated-report})
        (db/create-match validated-report)
        (redirect-after-post "/matches"))
      (layout/common (match/form (db/get-players) validated-report)))))

(defn matches-page []
  (layout/common (match/table (db/get-matches))))

(defn stats-players [sort order]
  (layout/common (stats/player-table (db/get-matches) :sort (keyword sort) :order (keyword order))))

(defn stats-teams [sort order]
  (layout/common (stats/team-table (db/get-matches) :sort (keyword sort) :order (keyword order))))

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

(defroutes home-routes
  (GET "/" [] (redirect "/report/match"))

  (GET "/report/match" [] (report-match-page))
  (POST "/report/match" request (report-match request))

  (GET "/matches" [] (matches-page))

  (GET "/stats/players" [sort order] (stats-players sort order))
  (GET "/stats/teams"   [sort order] (stats-teams sort order))

  (GET "/admin" [] (admin-page))
  (POST "/admin/player/add" [playername] (add-player playername))
  (POST "/admin/player/remove" [playerid] (remove-player playerid))
  (POST "/admin/match/remove" [matchid] (remove-match matchid)))
