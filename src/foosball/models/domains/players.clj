(ns foosball.models.domains.players
  (:require [datomic.api :as d]
            [foosball.util :as util]))

(defn get-by-id [dbc id]
  (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] dbc id)
       ffirst))

(defn get-all [dbc]
  (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pe :player/id ?pid]
              [?pe :player/name ?n]
              [?pe :player/active ?a]
              [?pe :user/role ?role]] dbc)
       (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
       (sort-by :name)))

(defn create! [conn uuid name openid]
  (let [eid (d/tempid :db.part/user)]
    @(d/transact conn
                 [{:db/id eid :player/id uuid}
                  {:db/id eid :player/name name}
                  {:db/id eid :player/active true}
                  {:db/id eid :user/openids openid}
                  {:db/id eid :user/role :user}])))

(defn entity-id-from-id [dbc uuid]
  (->> (d/q '[:find ?eid :in $ ?id :where [?eid :player/id ?id]] dbc uuid)
       ffirst))

(defn rename! [conn id newplayername]
  (let [eid (entity-id-from-id (d/db conn) id)]
    @(d/transact conn [{:db/id eid :player/name newplayername}])))

(defn activate! [conn id]
  (let [eid (entity-id-from-id (d/db conn) id)]
    @(d/transact conn [{:db/id eid :player/active true}])))

(defn deactivate! [conn id]
  (let [eid (entity-id-from-id (d/db conn) id)]
    @(d/transact conn [{:db/id eid :player/active false}])))

(defn set-player-role! [conn id role]
  (let [eid (entity-id-from-id (d/db conn) id)]
    @(d/transact conn [{:db/id eid :user/role role}])))
