(ns foosball.routes.home
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.views.player :as player]
            [foosball.util :as util]))

(defn home-page []
  (layout/common "Welcome to foosball"))

(defn match-page []
  (layout/common (match/form)))

(defn player-page []
  (layout/common (player/form)))

(defn new-player [playername]
  (info {:new-player  playername})
  (redirect-after-post "/player"))

(defroutes home-routes
  (GET "/" [] (home-page))

  (GET "/match" [] (match-page))
  (POST "/match" request (match/report request))

  (GET "/player" [] (player-page))
  (POST "/newplayer" [playername] (new-player playername)))
