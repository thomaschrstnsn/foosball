(ns foosball.models.domains.players
  (:require [datomic.api :as d]
            [foosball.util :as util]))

(defn get-by-id [dbc id]
  (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] dbc id)
       ffirst))

(defn get-all [dbc]
  (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pid :player/name ?n]
              [?pid :player/active ?a]
              [?pid :user/role ?role]] dbc)
       (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
       (sort-by :name)))

(defn create! [conn name openid]
  (let [playerid (d/tempid :db.part/user)
        result   @(d/transact conn
                              [{:db/id playerid :player/name name}
                               {:db/id playerid :player/active true}
                               {:db/id playerid :user/openids openid}
                               {:db/id playerid :user/role :user}])]
      (-> result :tempids first second)))

(defn rename! [conn id newplayername]
  @(d/transact conn [{:db/id id :player/name newplayername}]))

(defn activate! [conn id]
  @(d/transact conn [{:db/id id :player/active true}]))

(defn deactivate! [conn id]
  @(d/transact conn [{:db/id id :player/active false}]))

(defn set-player-role! [dbc id role]
  @(d/transact dbc [{:db/id id :user/role role}]))
