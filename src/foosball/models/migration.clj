(ns foosball.models.migration
  (:use [datomic.api :only [q db] :as d])
  (:use [clojure.set :only [difference]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util :as util]))

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

(defn- get-leagues [dbc]
  (->> (d/q '[:find ?lid ?ln :where [?lid :league/name ?ln]] dbc)
       (map (fn [[id name]] (util/symbols-as-map id name)))
       set))

(defn- ensure-atleast-one-league-exists [conn]
  (let [dbc         (db conn)
        leagues     (get-leagues dbc)
        no-leagues? (empty? leagues)]
    (info {:ensure-atleast-one-league-exists no-leagues?})
    (when no-leagues?
      (let [league-name "VFL"]
        (info "creating league:" league-name)
        @(d/transact conn [{:db/id (d/tempid :db.part/user) :league/name league-name}])))))

(defn- ensure-all-players-have-atleast-one-league [conn]
  (let [dbc                      (db conn)
        default-league           (-> (get-leagues dbc) first)
        players-with-names       (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                      (map (fn [[id]] id))
                                      set)
        players-with-leagues     (->> (d/q '[:find ?pid :where [?pid :player/leagues _]] dbc)
                                      (map first)
                                      set)
        players-to-give-a-league (difference players-with-names players-with-leagues)
        transaction              (map (fn [pid] {:db/id pid :player/leagues (:id  default-league)})
                                      players-to-give-a-league)]
    (info (merge {:ensure-all-players-have-atleast-one-league players-to-give-a-league}
                 (util/symbols-as-map default-league)))
    @(d/transact conn transaction)))

(def migrations "seq of idempotent migration functions"
  {"ensure-players-users-have-roles"  ensure-players-users-have-roles
   "ensure-atleast-one-league-exists" ensure-atleast-one-league-exists
   "ensure-all-players-have-atleast-one-league" ensure-all-players-have-atleast-one-league})

(defn migrate-schema-and-data [conn]
  (doseq [[name migration-fn] migrations]
    (info "running migration:" name)
    (migration-fn conn)))
