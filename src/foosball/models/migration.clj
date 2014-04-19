(ns foosball.models.migration
  (:require [clojure.set :refer [difference]]
            [datomic.api :as d :refer [db]]
            [foosball.util :as util]
            [taoensso.timbre :refer [info]]))

(defn- ensure-players-users-have-roles
  "Ensure every player has the default role"
  [conn]
  (let [dbc                (db conn)
        players-with-names (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-with-roles (->> (d/q '[:find ?pid :where [?pid :user/role _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-to-default (difference players-with-names players-with-roles)
        transaction        (map (fn [id] {:db/id id :user/role :user}) players-to-default)]
    (info "Players changed:" (count players-to-default))
    @(d/transact conn transaction)))

(defn- ensure-players-have-ids
  "Ensure that every player has an :player/id attribute/value"
  [conn]
  (let [dbc                (db conn)
        players-with-name  (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-with-id    (->> (d/q '[:find ?pid :where [?pid :player/id _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-without-id (difference players-with-name players-with-id)
        transaction        (map (fn [id] {:db/id id :player/id (java.util.UUID/randomUUID)})
                                players-without-id)]
    (info "Players given id:" (count players-without-id))
    @(d/transact conn transaction)))

(defn- ensure-matches-have-ids
  "Ensure that every match has an :match/id attribute/value"
  [conn]
  (let [dbc                (db conn)
        matches-with-time  (->> (d/q '[:find ?mid :where [?mid :match/time _]] dbc)
                                (map (fn [[id]] id))
                                set)
        matches-with-id    (->> (d/q '[:find ?pid :where [?pid :match/id _]] dbc)
                                (map (fn [[id]] id))
                                set)
        matches-without-id (difference matches-with-time matches-with-id)
        transaction        (map (fn [id] {:db/id id :match/id (java.util.UUID/randomUUID)})
                                matches-without-id)]
    (info "Matches given id:" (count matches-without-id))
    @(d/transact conn transaction)))

(def migrations "seq of idempotent migration functions"
  [#'ensure-players-users-have-roles
   #'ensure-players-have-ids
   #'ensure-matches-have-ids])

(defn migrate-schema-and-data [conn]
  (doseq [migration-var migrations]
    (info "Running migration:" (:doc (meta migration-var)))
    (migration-var conn)))
