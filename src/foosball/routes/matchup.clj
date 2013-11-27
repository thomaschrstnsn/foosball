(ns foosball.routes.matchup
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]
        [compojure.core :only [GET POST]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.matchup :as matchup]
            [foosball.util :as util]
            [foosball.models.domains :as d]
            [foosball.auth :as auth]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(defn matchup-page
  ([{:keys [config-options db]}]
     (layout/common config-options
                    :title "Matchup" :content (matchup/page (d/get-players db)
                                                            (d/get-matches db))))
  ([{:keys [config-options db]} {:keys [params]}]
     (let [{:keys [playerids]} params]
       (layout/common config-options
                      :title "Matchup"
                      :content (matchup/page (d/get-players db)
                                             (d/get-matches db)
                                             (map util/parse-id playerids))))))

(defn routes [deps]
  (let [matchup-routes (compojure/routes
                        (GET  "/" []      (matchup-page deps))
                        (POST "/" request (matchup-page deps request)))]
    (compojure/context "/matchup" request
                       (friend/wrap-authorize matchup-routes #{auth/user}))))
