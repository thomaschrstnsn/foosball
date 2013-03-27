(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.views.player :as player]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn home-page []
  (layout/common "Welcome to foosball"))

(defn match-page []
  (layout/common (match/form)))

(defn player-page []
  (let [players (db/get-players)]
    (info players)
    (layout/common (player/form players))))

(defn add-player [name]
  (info {:add-player name})
  (db/create-player {:name name})
  (redirect-after-post "/player"))

(defn remove-player [id]
  (info {:remove-player id})
  (db/delete-player id)
  (redirect-after-post "/player"))

(defn report [{:keys [params]}]
  (info {:report-match-params params})
  (let [validated-report (->> params
                              match/parse-form
                              match/validate-report)]
    (if (->> validated-report :validation-errors empty?)
      (do
        (info "validated report")
        (warn "TODO: persist report")
        (redirect-after-post "/"))
      (layout/common (match/form validated-report)))))

(defroutes home-routes
  (GET "/" [] (home-page))

  (GET "/match" [] (match-page))
  (POST "/match" request (report request))

  (GET "/player" [] (player-page))
  (POST "/player/add" [playername] (add-player playername))
  (POST "/player/remove" [playerid] (remove-player playerid)))
