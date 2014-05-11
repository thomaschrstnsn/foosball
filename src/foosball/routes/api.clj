(ns foosball.routes.api
  (:require [liberator.core :refer [resource defresource]]
            [cemerick.friend :as friend]
            [compojure.core :as compojure :refer [ANY GET POST PUT]]
            [foosball.auth :as auth]
            [foosball.models.domains :as d]
            [foosball.statistics.ratings :as ratings]
            [foosball.statistics.team-player :as player]
            [foosball.util :as util]
            [clojure.data.json :as json]))

(extend java.util.UUID
  json/JSONWriter
  {:-write (fn [obj out]
             (json/-write (str obj) out))})

(def media-types ["application/edn" "text/html" "application/json"])

(defresource players [db]
  :available-media-types media-types
  :handle-ok (fn [_] (d/get-players db)))

(defresource leaderboard [db size]
  :available-media-types media-types
  :handle-ok (fn [_] (let [players           (d/get-players db)
                          matches           (d/get-matches db)
                          stats             (player/calculate-player-stats matches)
                          log-and-ratings   (ratings/ratings-with-log players matches)
                          ratings           (:ratings log-and-ratings)
                          logs              (:logs log-and-ratings)
                          won-matches       (:won-matches log-and-ratings)
                          form-by-player    (ratings/calculate-form-from-matches won-matches 5)
                          stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                                   (merge stat
                                                          {:rating (ratings player)}
                                                          {:form   (form-by-player player)}))
                                                 stats)]
                      (->> stats-and-ratings
                           (sort-by :rating)
                           (reverse)
                           (take size)
                           (map (fn [index player] {:position (inc index)
                                                   :player/name (:player player)
                                                   :form (map {true :won false :lost} (:form player))
                                                   :rating (:rating player)})
                                (range))))))

(defn routes [{:keys [db]}]
  (let [player-route (ANY "/api/players" [] (players db))]
    (compojure/routes
     (compojure/context "/private"
                        request
                        (friend/wrap-authorize (compojure/routes player-route)
                                               #{auth/user}))
     player-route
     (GET "/api/ratings/leaderboard/:n" [n] (leaderboard db (or (util/parse-int n) 5))))))
