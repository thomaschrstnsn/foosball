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

(def ^:const ^:private default-league-name "VFL")

(defn- get-default-league [dbc]
  (->> (get-leagues dbc)
      (filter (fn [{:keys [name]}] (= name default-league-name)))
      first))

(defn- ensure-atleast-one-league-exists [conn]
  (let [dbc         (db conn)
        leagues     (get-leagues dbc)
        no-leagues? (empty? leagues)]
    (info {:ensure-atleast-one-league-exists no-leagues?})
    (when no-leagues?
      (info "creating league:" default-league-name)
      @(d/transact conn [{:db/id (d/tempid :db.part/user) :league/name default-league-name}]))))

(defn- ensure-all-players-have-atleast-one-league [conn]
  (let [dbc                      (db conn)
        default-league           (get-default-league dbc)
        players-with-names       (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                      (map (fn [[id]] id))
                                      set)
        players-with-leagues     (->> (d/q '[:find ?pid :where [?pid :player/leagues _]] dbc)
                                      (map first)
                                      set)
        players-to-give-a-league (difference players-with-names players-with-leagues)
        transaction              (map (fn [pid] {:db/id pid :player/leagues (:id  default-league)})
                                      players-to-give-a-league)]
    (info (merge {:ensure-all-players-have-atleast-one-league players-to-give-a-league
                  :count (count players-to-give-a-league)}
                 (util/symbols-as-map default-league)))
    @(d/transact conn transaction)))

(defn- ensure-all-matches-happened-in-a-league [conn]
  (let [dbc                      (db conn)
        default-league           (get-default-league dbc)
        matches-played           (->> (d/q '[:find ?mid :where [?mid :match/time _]] dbc)
                                      (map first)
                                      set)
        matches-with-leagues     (->> (d/q '[:find ?mid :where [?mid :match/league _]] dbc)
                                      (map first)
                                      set)
        matches-to-give-a-league (difference matches-played matches-with-leagues)
        transaction              (map (fn [mid] {:db/id mid :match/league (:id default-league)})
                                      matches-to-give-a-league)]
    (info (merge {:ensure-all-matches-happened-in-a-league matches-to-give-a-league
                  :count (count matches-to-give-a-league)}
                 (util/symbols-as-map default-league)))
    @(d/transact conn transaction)))

(def migrations "seq of idempotent migration functions"
  [["ensure-players-users-have-roles" ensure-players-users-have-roles]
   ["ensure-atleast-one-league-exists" ensure-atleast-one-league-exists]
   ["ensure-all-players-have-atleast-one-league" ensure-all-players-have-atleast-one-league]
   ["ensure-all-matches-happened-in-a-league" ensure-all-matches-happened-in-a-league]])

(defn migrate-schema-and-data [conn]
  (doseq [[name migration-fn] migrations]
    (info "running migration:" name)
    (migration-fn conn)))
