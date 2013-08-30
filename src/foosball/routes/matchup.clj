(ns foosball.routes.matchup
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.matchup :as matchup]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn matchup-page
  ([]
     (layout/common "Matchup" (matchup/page (db/get-players) (db/get-matches))))
  ([{:keys [params]}]
     (let [{:keys [playerids]} params]
       (layout/common "Matchup" (matchup/page (db/get-players) (db/get-matches) (map util/parse-id playerids))))))

(defroutes matchup-routes
  (GET  "/matchup" []      (matchup-page))
  (POST "/matchup" request (matchup-page request)))
