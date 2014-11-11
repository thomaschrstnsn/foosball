(ns foosball.models.domains.players
  (:require [datomic.api :as d]
            [schema.core :as s]
            [foosball.models.domains.helpers :as h]
            [foosball.util :as util]
            [foosball.entities :as e]))

(defn player-query [by-id?]
  (let [find     '[:find ?pid ?n ?a ?role]
        in       (if by-id?
                   '[:in $ ?id]
                   '[:in $])
        where    '[:where
                   [?pe :player/id ?pid]
                   [?pe :player/name ?n]
                   [?pe :player/active ?a]
                   [?pe :user/role ?role]]
        id-query (when by-id? '[[?pe :player/id ?id]])]
    (vec (concat find in where id-query))))

(defn db-result-to-User [[id name active role]]
  (util/identity-map id name active role))

(s/defn get-by-id :- (s/maybe e/User)
  [dbc
   id :- s/Uuid]
  (->> (d/q (player-query true) dbc id)
       (map db-result-to-User)
       first))

(s/defn get-all :- [e/User]
  [dbc]
  (->> (d/q (player-query false) dbc)
       (map db-result-to-User)
       (sort-by :name)))

(s/defn create!
  [conn
   uuid :- s/Uuid
   name :- s/Str
   openid :- s/Str]
  (let [_ (when (h/entity-id-from-attr-value (d/db conn) :player/id uuid)
            (throw (Exception. ":player/id is not unique")))
        eid (d/tempid :db.part/user)]
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
