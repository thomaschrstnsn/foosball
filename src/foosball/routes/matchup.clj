(ns foosball.routes.matchup
  (:require [cemerick.friend :as friend]
            [compojure.core :as compojure :refer [GET POST]]
            [foosball.auth :as auth]
            [foosball.models.domains :as d]
            [foosball.util :as util]
            [foosball.views.layout :as layout]
            [foosball.views.matchup :as matchup]))

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
