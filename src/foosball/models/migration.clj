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

(def migrations "seq of idempotent migration functions"
  [#'ensure-players-users-have-roles])

(defn migrate-schema-and-data [conn]
  (doseq [migration-var migrations]
    (info "Running migration:" (:doc (meta migration-var)))
    (migration-var conn)))
