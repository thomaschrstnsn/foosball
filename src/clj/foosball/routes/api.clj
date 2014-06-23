(ns foosball.routes.api
  (:require [liberator.core :refer [resource defresource]]
            [cemerick.friend :as friend]
            [compojure.core :as compojure :refer [ANY GET POST PUT]]
            [foosball.auth :as auth]
            [foosball.models.domains :as d]
            [foosball.statistics.ratings :as ratings]
            [foosball.statistics.team-player :as team-player]
            [foosball.util :as util]
            [foosball.software :as sw]
            [foosball.auth :as auth]
            [clojure.data.json :as json]))

(extend java.util.UUID
  json/JSONWriter
  {:-write (fn [obj out]
             (json/-write (str obj) out))})

(def media-types ["application/edn" "text/html" "application/json"])

(defresource players [db]
  :available-media-types media-types
  :handle-ok (fn [_] (d/get-players db)))

(defresource matches [db]
  :available-media-types media-types
  :handle-ok (fn [_] (d/get-matches db)))

(defresource leaderboard [db size]
  :available-media-types media-types
  :handle-ok (fn [_] (let [players (d/get-players db)
                          matches (d/get-matches db)]
                      (ratings/leaderboard matches players size))))

(defresource player-log [db playerid]
  :available-media-types media-types
  :handle-ok (fn [_]
               (let [matches             (d/get-matches db)
                     playername          (d/get-player db playerid)
                     log                 (ratings/calculate-reduced-log-for-player playername matches)
                     activity-log-keys   [:log-type :matchdate :team-mate :opponents
                                          :expected :win? :delta :new-rating]
                     inactivity-log-keys [:log-type :inactivity :delta :new-rating]]
                 (map (fn [{:keys [log-type] :as l}]
                        (select-keys l
                                     (if (not= :inactivity log-type)
                                       activity-log-keys
                                       inactivity-log-keys)))
                      (reverse log)))))

(defresource player-stats [db]
  :available-media-types media-types
  :handle-ok (fn [_]
               (let [players (d/get-players db)
                     matches (d/get-matches db)]
                 (ratings/calculate-player-stats-table matches players))))

(defresource team-stats [db]
  :available-media-types media-types
  :handle-ok (fn [_]
               (let [matches (d/get-matches db)]
                 (team-player/calculate-team-stats matches))))

(defresource login-status []
  :available-media-types media-types
  :handle-ok (fn [_]
               (let [a    (auth/current-auth)
                     name (str (:firstname a) " " (:lastname a))
                     existy? (fn [x] (not (nil? x)))]
                 (merge
                  {:logged-in (existy? a)}
                  (when a
                    {:user?       (existy? (auth/user?))
                     :admin?      (existy? (auth/admin?))
                     :name        name

                     :logout-form (auth/logout-form :extra-class "navbar-form"
                                                    :text (str "Logout")
                                                    :title name)})
                  (when-not a
                    {:login-form  (auth/login-form :form-class "navbar-form")})))))

(defresource about-software [project]
  :available-media-types media-types
  :handle-ok (fn [_]
               (sw/software-dependencies project)))

(defn routes [{:keys [db project]}]
  (let [player-route (GET "/api/players" [] (players db))]
    (compojure/routes
     (compojure/context "/private"
                        request
                        (friend/wrap-authorize (compojure/routes player-route)
                                               #{auth/user}))
     player-route
     (GET "/api/matches" [] (matches db))
     (GET "/api/ratings/leaderboard/:n" [n] (leaderboard db (or (util/parse-int n) 5)))
     (GET "/api/ratings/log/:playerid" [playerid] (player-log db (util/uuid-from-string playerid)))
     (GET "/api/ratings/player-stats" [] (player-stats db))
     (GET "/api/ratings/team-stats" [] (team-stats db))
     (GET "/api/login/status" [] (login-status))
     (GET "/api/about/software" [] (about-software project)))))
