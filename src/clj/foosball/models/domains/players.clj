(ns foosball.models.domains.players
  (:require [datomic.api :as d]
            [schema.core :as s]
            [foosball.models.domains.helpers :as h]
            [foosball.util :as util]
            [foosball.entities :as e]))

(s/defn get-by-id :- (s/maybe s/Str)
  [dbc
   id :- s/Uuid]
  (->> (d/q '[:find ?player :in $ ?id :where
              [?ent :player/id ?id]
              [?ent :player/name ?player]] dbc id)
       ffirst))

(s/defn get-all :- [e/User]
  [dbc]
  (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pe :player/id ?pid]
              [?pe :player/name ?n]
              [?pe :player/active ?a]
              [?pe :user/role ?role]] dbc)
       (map (fn [[id name active role]] (util/identity-map id name active role)))
       (sort-by :name)))

(s/defn create!
  [conn
   uuid :- s/Uuid
   name :- s/Str
   openid :- s/Str]
  (let [eid (d/tempid :db.part/user)]
    @(d/transact conn
                 [{:db/id eid :player/id uuid}
                  {:db/id eid :player/name name}
                  {:db/id eid :player/active true}
                  {:db/id eid :user/openids openid}
                  {:db/id eid :user/role :user}])))

(defn entity-id-from-id [dbc uuid]
  (h/entity-id-from-attr-value dbc :player/id uuid))

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
