(ns foosball.models.domains.openids
  (:require [datomic.api :as d :refer [db]]
            [foosball.models.domains.helpers :as h]
            [foosball.models.domains.players :as players]
            [foosball.util :as util]))

(defn get-all-with-openid [dbc]
  (->> (d/q '[:find ?pid ?name
              :where
              [?pe :player/name ?name]
              [?pe :player/id ?pid]
              [?pe :user/openids _]]
            dbc)
       (map (fn [[id name]] (util/identity-map id name)))))

(defn get-all-without-openid [dbc]
  (let [playerids-with-openid (->> (get-all-with-openid dbc)
                                   (map (fn [{:keys [id]}] id))
                                   (set))]
    (->> (players/get-all dbc)
         (filter (fn [{:keys [id]}] (not (playerids-with-openid id)))))))

(defn get-player-openids [dbc id]
  (->> (d/q '[:find ?openids :in $ ?id :where
              [?ent :player/id ?id]
              [?ent :user/openids ?openids]]
            dbc id)
       (mapcat identity)
       (set)))

(defn add-openid-to-player! [conn playerid openid]
  (let [entity-id (h/entity-id-from-attr-value (d/db conn) :player/id playerid)]
    @(d/transact conn [{:db/id entity-id :user/openids openid}])))

(defn remove-openids-from-player! [conn playerid openids]
  (let [entity-id (h/entity-id-from-attr-value (d/db conn) :player/id playerid)]
    @(d/transact conn (vec (for [openid openids]
                             [:db/retract entity-id :user/openids openid])))))

(defn remove-player-openids! [conn id]
  (remove-openids-from-player! conn id (get-player-openids (db conn) id)))

(defn get-player-with-given-openid [dbc openid]
  (->> (d/q '[:find ?pid ?name ?role
              :in $ ?openid
              :where
              [?ent :player/id ?pid]
              [?ent :player/name ?name]
              [?ent :user/openids ?openid]
              [?ent :user/role ?role]]
            dbc openid)
       (map (fn [[playerid playername playerrole]] (util/identity-map playerid playername playerrole)))
       first))
