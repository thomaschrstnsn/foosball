(ns foosball.models.migration
  (:use [datomic.api :only [q db] :as d])
  (:use [clojure.set :only [difference]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]]))

(defn- ensure-players-users-have-roles [conn]
  (let [dbc                (db conn)
        players-with-names (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-with-roles (->> (d/q '[:find ?pid :where [?pid :user/role _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-to-default (difference players-with-names players-with-roles)
        transaction        (map (fn [id] {:db/id id :user/role :user}) players-to-default)]
    (info {:ensure-players-users-have-roles transaction})
    @(d/transact conn transaction)))

(def migrations "seq of idempotent migration functions"
  {"ensure-players-users-have-roles"  ensure-players-users-have-roles})

(defn migrate-schema-and-data [conn]
  (doseq [[name migration-fn] migrations]
    (info "running migration:" name)
    (migration-fn conn)))
