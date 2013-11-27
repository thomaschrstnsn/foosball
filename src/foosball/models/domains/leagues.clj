(ns foosball.models.domains.leagues
  (:use [datomic.api :only [q db] :as d])
  (:require [foosball.util :as util]))

(defn get-leagues-for-player [dbc playerid]
  (->> (d/q '[:find ?lid ?name
              :in $ ?playerid
              :where
              [?playerid :player/leagues ?lid]
              [?lid :league/name ?name]] dbc playerid)
       (map (fn [[id name]] (util/symbols-as-map id name)))))

(defn get-players-in-league [dbc leagueid]
  (->> (d/q '[:find ?pid ?n ?a ?role
                :in $ ?leagueid
                :where
                [?pid :player/name ?n]
                [?pid :player/leagues ?leagueid]
                [?pid :player/active ?a]
                [?pid :user/role ?role]] dbc leagueid)
         (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
         (sort-by :name)))

(defn get-all [dbc]
  (->> (d/q '[:find ?lid ?name :where
              [?lid :league/name ?name]] dbc)
       (map (fn [[id name]] (util/symbols-as-map id name)))))

(defn add-player-to-league! [conn player-id league-id]
  @(d/transact conn [{:db/id player-id :player/leagues league-id}]))

(defn add! [conn league-name]
  @(d/transact conn [{:db/id (d/tempid :db.part/user) :league/name league-name}]))

(defn rename! [conn league-id new-name]
  @(d/transact conn [{:db/id league-id :league/name new-name}]))
