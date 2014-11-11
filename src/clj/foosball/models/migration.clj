(ns foosball.models.migration
  (:require [clojure.set :refer [difference]]
            [datomic.api :as d :refer [db]]
            [foosball.util :as util]
            [taoensso.timbre :refer [info]]))

(defn- ensure-players-users-have-roles
  "Ensure every player has the default role"
  [conn]
  (let [dbc                (db conn)
        players-with-names (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-with-roles (->> (d/q '[:find ?pid :where [?pid :user/role _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-to-default (difference players-with-names players-with-roles)
        transaction        (map (fn [id] {:db/id id :user/role :user}) players-to-default)]
    (info "Players changed:" (count players-to-default))
    @(d/transact conn transaction)))

(defn- ensure-players-have-ids
  "Ensure that every player has an :player/id attribute/value"
  [conn]
  (let [dbc                (db conn)
        players-with-name  (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-with-id    (->> (d/q '[:find ?pid :where [?pid :player/id _]] dbc)
                                (map (fn [[id]] id))
                                set)
        players-without-id (difference players-with-name players-with-id)
        transaction        (map (fn [id] {:db/id id :player/id (java.util.UUID/randomUUID)})
                                players-without-id)]
    (info "Players given id:" (count players-without-id))
    @(d/transact conn transaction)))

(defn- ensure-matches-have-ids
  "Ensure that every match has an :match/id attribute/value"
  [conn]
  (let [dbc                (db conn)
        matches-with-time  (->> (d/q '[:find ?mid :where [?mid :match/time _]] dbc)
                                (map (fn [[id]] id))
                                set)
        matches-with-id    (->> (d/q '[:find ?pid :where [?pid :match/id _]] dbc)
                                (map (fn [[id]] id))
                                set)
        matches-without-id (difference matches-with-time matches-with-id)
        transaction        (map (fn [id] {:db/id id :match/id (java.util.UUID/randomUUID)})
                                matches-without-id)]
    (info "Matches given id:" (count matches-without-id))
    @(d/transact conn transaction)))

(defn remove-uniqueness-constraint-on-player-name
  "Removes the uniqueness constraint on :player/name"
  [conn]
  @(d/transact conn [[:db/retract :player/name :db/unique :db.unique/identity]
                     [:db/add :db.part/db :db.alter/attribute :player/name]]))

(defn add-league! [conn name description]
  (let [tempid    (d/tempid :db.part/user)
        _         (info "Adding league: " (util/identity-map name description))
        res       @(d/transact conn [{:db/id tempid :league/id (util/create-uuid)}
                                     {:db/id tempid :league/name name}
                                     {:db/id tempid :league/description description}])
        league-id (d/resolve-tempid (:db-after res) (:tempids res) tempid)
        _         (info "league entity-id" league-id)]
    league-id))

(defn add-vfl-league!
  "Adds VFL league (idempotent)"
  [conn]
  (let [dbc         (db conn)
        league-name "VFL"
        vfl-leagues (d/q '[:find ?lid :in $ ?name :where [?lid :league/name ?name]] dbc league-name)]
    (condp = (count vfl-leagues)
      0 (add-league! conn league-name "Videncentret For Landbrug")
      1 (ffirst vfl-leagues)
      (throw (Exception. "More than one VFL league found")))))

(defn set-vfl-league-for-all-matches!
  "Adds the VFL league and sets it for all matches without leagues"
  [conn]
  (let [league-entity-id        (add-vfl-league! conn)
        dbc                     (db conn)
        matches-with-leagues    (->> (d/q '[:find ?mid :where [?mid :match/league _]] dbc)
                                     (map first)
                                     set)
        all-matches             (->> (d/q '[:find ?mid :where [?mid :match/id _]] dbc)
                                     (map first)
                                     set)
        matches-without-leagues (difference all-matches matches-with-leagues)
        __                      (info "vfl league entity-id" league-entity-id)
        _                       (info "adding vfl as league for" (count matches-without-leagues) "matches")
        transaction             (mapv (fn [id] {:db/id id :match/league league-entity-id})
                                      matches-without-leagues)]
    @(d/transact conn transaction)))

(def migrations "seq of idempotent migration functions"
  [#'ensure-players-users-have-roles
   #'ensure-players-have-ids
   #'ensure-matches-have-ids
   #'remove-uniqueness-constraint-on-player-name
   #'set-vfl-league-for-all-matches!])

(defn migrate-schema-and-data [conn]
  (doseq [migration-var migrations]
    (info "Running migration:" (:doc (meta migration-var)))
    (migration-var conn)))
