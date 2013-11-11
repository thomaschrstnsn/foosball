(ns foosball.routes.matchup
  (:use [compojure.core :only [defroutes context GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.matchup :as matchup]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [cemerick.friend :as friend]
            [foosball.auth :as auth]))

(defn matchup-page
  ([]
     (layout/common :title "Matchup" :content (matchup/page (db/get-players) (db/get-matches))))
  ([{:keys [params]}]
     (let [{:keys [playerids]} params]
       (layout/common :title "Matchup"
                      :content (matchup/page (db/get-players)
                                             (db/get-matches)
                                             (map util/parse-id playerids))))))

(defroutes unprotected
  (GET  "/" []      (matchup-page))
  (POST "/" request (matchup-page request)))

(defroutes routes
  (context "/matchup" request
           (friend/wrap-authorize unprotected #{auth/user})))
