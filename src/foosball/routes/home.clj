(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.views.player :as player]
            [foosball.util :as util]))

(defn home-page []
  (layout/common "Welcome to foosball"))

(defn match-page []
  (layout/common (match/form)))

(defn report-match [args]
  (println args)
  (redirect-after-post "/"))

(defn player-page []
  (layout/common (player/form)))

(defn new-player [playername]
  (println "adding player:" playername)
  (redirect-after-post "/player"))

(defroutes home-routes
  (GET "/" [] (home-page))

  (GET "/match" [] (match-page))
  (POST "/match" request (report-match request))

  (GET "/player" [] (player-page))
  (POST "/newplayer" [playername] (new-player playername)))
