(ns foosball.models.domains.openids
  (:use [datomic.api :only [q db] :as d])
  (:require [foosball.util :as util]
            [foosball.models.domains.players :as players]))

(defn get-all-with-openid [dbc]
  (->> (d/q '[:find ?pid ?name
              :where
              [?pid :player/name ?name]
              [?pid :user/openids _]]
            dbc)
       (map (fn [[id name]] (util/symbols-as-map id name)))))

(defn get-all-without-openid [dbc]
  (let [playerids-with-openid (->> (get-all-with-openid dbc)
                                   (map (fn [{:keys [id]}] id))
                                   (set))]
    (->> (players/get-all dbc)
         (filter (fn [{:keys [id]}] (not (playerids-with-openid id)))))))

(defn get-player-openids [dbc id]
  (->> (d/q '[:find ?openids :in $ ?id :where [?id :user/openids ?openids]]
            dbc id)
       (mapcat identity)
       (set)))

(defn add-openid-to-player! [conn playerid openid]
  @(d/transact conn [{:db/id playerid :user/openids openid}]))

(defn remove-openids-from-player! [conn playerid openids]
  @(d/transact conn (vec (for [openid openids]
                           [:db/retract playerid :user/openids openid]))))

(defn remove-player-openids! [conn id]
  (remove-openids-from-player! conn id (get-player-openids (db conn) id)))

(defn get-player-with-given-openid [dbc openid]
  (->> (d/q '[:find ?pid ?name ?role
              :in $ ?openid
              :where
              [?pid :player/name ?name]
              [?pid :user/openids ?openid]
              [?pid :user/role ?role]]
            dbc openid)
       (map (fn [[playerid playername playerrole]] (util/symbols-as-map playerid playername playerrole)))
       first))
