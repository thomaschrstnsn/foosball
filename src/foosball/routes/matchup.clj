(ns foosball.routes.matchup
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.matchup :as matchup]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [foosball.auth :as auth]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(defn matchup-page
  ([database]
     (layout/common :title "Matchup" :content (matchup/page (db/get-players-db database)
                                                            (db/get-matches-db database))))
  ([database {:keys [params]}]
     (let [{:keys [playerids]} params]
       (layout/common :title "Matchup"
                      :content (matchup/page (db/get-players-db database)
                                             (db/get-matches-db database)
                                             (map util/parse-id playerids))))))
(defn routes [database]
  (let [matchup-routes (compojure/routes
                        (compojure/GET  "/" []      (matchup-page database))
                        (compojure/POST "/" request (matchup-page database request)))]
    (compojure/context "/matchup" request
                       (friend/wrap-authorize matchup-routes #{auth/user}))))
