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
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(extend java.util.UUID
  json/JSONWriter
  {:-write (fn [obj out]
             (json/-write (str obj) out))})

;; media types
(def edn-type "application/edn")
(def json-type "application/json")
(def media-types [edn-type "text/html" json-type])
(def body-media-types [edn-type json-type])

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

(defresource matchup [db playerids]
  :available-media-types media-types
  :malformed? (fn [_] (let [players         (d/get-players db)
                           request-players (set playerids)
                           valid-playerids (->> (map :id players)
                                                (filter (fn [pid] (request-players pid)))
                                                set)]
                       (> 4 (count valid-playerids))))
  :handle-ok  (fn [_] (let [matches          (d/get-matches db)
                           players          (d/get-players db)
                           request-players  (set playerids)
                           selected-players (filter (fn [{:keys [id]}] (contains? request-players id)) players)]
                       (ratings/calculate-matchup matches selected-players))))

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

(defresource auth-status []
  :available-media-types media-types
  :handle-ok (fn [_]
               (let [a       (auth/current-auth)
                     name    (str (:firstname a) " " (:lastname a))
                     existy? (fn [x] (not (nil? x)))
                     truthy? (fn [x] (if x true false))]
                 (merge
                  {:logged-in? (existy? a)}
                  (when a
                    {:user?    (truthy? (auth/user?))
                     :admin?   (truthy? (auth/admin?))
                     :username name})
                  (when-not a
                    {:provider auth/provider})))))

(defresource about-software [project]
  :available-media-types media-types
  :handle-ok (fn [_]
               (sw/software-dependencies project)))

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (json/read-str body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

(defn parse-edn [context key]
  nil)

;; For PUT and POST check if the content type is json.
(defn check-content-type [content-types ctx]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

(defresource match-report-resource [id]
  :allowed-methods [:get :put :delete]
  :available-media-types media-types
  :known-content-type? (partial check-content-type body-media-types)
  :exists? (fn [_]
             false
             #_ (let [e (get @entries id)]
                    (if-not (nil? e)
                      {::entry e})))
  :existed? (fn [_] false #_ (nil? (get @entries id ::sentinel)))
  :handle-ok ::entry
  :delete! (fn [_] false #_ (dosync (alter entries assoc id nil)))
  :malformed? #(parse-json % ::data)
  :can-put-to-missing? false
  :put! true #_ #(dosync (alter entries assoc id (::data %)))
  :new? true #_ (fn [_] (nil? (get @entries id ::sentinel))))

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
     (GET "/api/matchup" [& players]
          (matchup db (map util/uuid-from-string (vals players))))
     (GET "/api/auth" [] (auth-status))
     (GET "/api/about/software" [] (about-software project)))))
