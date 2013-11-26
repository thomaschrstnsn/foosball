(ns foosball.models.db
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [datomic.api :only [q db] :as d])
  (:use [foosball.models.schema :only [eav-schema]])
  (:use [clojure.set :only [difference]])
  (:require [foosball.util :as util]
            [foosball.models.migration :as migration]
            [com.stuartsierra.component :as component]))

(def ^:dynamic ^:private conn)
(def ^:dynamic ^:private *uri*)

(defn create-db-and-connect [uri]
  (alter-var-root #'*uri* (constantly uri))
  (info "creating database on uri:" *uri*)
  (d/create-database *uri*)

  (info "connecting to database")

  (alter-var-root #'conn (constantly (d/connect *uri*)))

  (info "transacting schema")
  @(d/transact conn eav-schema)

  (migration/migrate-schema-and-data conn)

  (info "database initialized")
  conn)

(defn delete-db-and-disconnect []
  (d/delete-database *uri*)
  (alter-var-root #'conn  (constantly nil))
  (alter-var-root #'*uri* (constantly nil)))

(defn create-player [name openid]
  (let [playerid (d/tempid :db.part/user)
        result    @(d/transact conn [{:db/id playerid :player/name name}
                                     {:db/id playerid :player/active true}
                                     {:db/id playerid :user/openids openid}
                                     {:db/id playerid :user/role :user}])]
    (-> result :tempids first second)))

(defn rename-player [id newplayername]
  @(d/transact conn [{:db/id id :player/name newplayername}]))

(defn set-players-role [id role]
  @(d/transact conn [{:db/id id :user/role role}]))

(defn get-players []
  (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pid :player/name ?n]
              [?pid :player/active ?a]
              [?pid :user/role ?role]] (db conn))
       (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
       (sort-by :name)))

;; leagues

(defn get-leagues []
  (->> (d/q '[:find ?lid ?name :where
              [?lid :league/name ?name]] (db conn))
       (map (fn [[id name]] (util/symbols-as-map id name)))))

(defn add-league [league-name]
  @(d/transact conn [{:db/id (d/tempid :db.part/user) :league/name league-name}]))

(defn rename-league [league-id new-name]
  @(d/transact conn [{:db/id league-id :league/name new-name}]))

;; players and leagues

(defn add-player-to-league [player-id league-id]
  @(d/transact conn [{:db/id player-id :player/leagues league-id}]))





;; openid

(defn remove-openids-from-player [playerid openids]
  @(d/transact conn (vec (for [openid openids]
                           [:db/retract playerid :user/openids openid]))))

(defn get-players-openids [id]
  (->> (d/q '[:find ?openids :in $ ?id :where [?id :user/openids ?openids]] (db conn) id)
       (mapcat identity)
       (set)))

(defn remove-players-openids [id]
  (remove-openids-from-player id (get-players-openids id)))

(defn get-player-with-given-openid [openid]
  (->> (d/q '[:find ?pid ?name ?role
              :in $ ?openid
              :where
              [?pid :player/name ?name]
              [?pid :user/openids ?openid]
              [?pid :user/role ?role]]
            (db conn) openid)
       (map (fn [[playerid playername playerrole]] (util/symbols-as-map playerid playername playerrole)))
       first))

;; match


(defn get-player
  ([id] (get-player id (db conn)))
  ([id dbc]
     (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] dbc id)
          ffirst)))

(defn get-team
  ([id] (get-team id (db conn)))
  ([id dbc]
     (->> (d/q '[:find ?p1 ?p2 ?score
                 :in $ ?id :where
                 [?id :team/player1 ?p1]
                 [?id :team/player2 ?p2]
                 [?id :team/score ?score]] dbc id)
          (map (fn [[p1 p2 score]] {:player1 (get-player p1 dbc)
                                   :player2 (get-player p2 dbc)
                                   :score score}))
          first)))

(defn get-reported-by
  ([matchid] (get-reported-by matchid (db conn)))
  ([matchid dbc]
     (->> (d/q '[:find ?n ?pid
                 :in $ ?matchid
                 :where
                 [?matchid :match/reported-by ?pid]
                 [?pid     :player/name       ?n]] dbc matchid)
          (map (fn [[name id]] name))
          first)))

(defn get-matches []
  (let [dbc (db conn)]
    (->> (d/q '[:find ?m ?mt ?t1 ?t2 ?tx
                :in $
                :where
                [?m :match/time  ?mt ?tx]
                [?m :match/team1 ?t1]
                [?m :match/team2 ?t2]
                [?t1 :team/player1 _]
                [?t1 :team/player2 _]
                [?t1 :team/score   _]
                [?t2 :team/player1 _]
                [?t2 :team/player2 _]
                [?t2 :team/score   _]] dbc)
         (map (fn [[mid mt t1 t2 tx]]
                {:id mid :matchdate mt :tx tx
                 :team1 (merge {:id t1} (get-team t1 dbc))
                 :team2 (merge {:id t2} (get-team t2 dbc))
                 :reported-by (get-reported-by mid dbc)}))
         (sort-by (juxt :matchdate :tx)))))

(defn get-matches-in-league [league-id]
  (let [dbc (db conn)]
    (->> (d/q '[:find ?m ?mt ?t1 ?t2 ?tx
                :in $ ?league-id
                :where
                [?m :match/time  ?mt ?tx]
                [?m :match/team1 ?t1]
                [?m :match/team2 ?t2]
                [?m :match/league ?league-id]
                [?t1 :team/player1 ?t1p1]
                [?t1 :team/player2 ?t1p2]
                [?t1 :team/score   _]
                [?t2 :team/player1 ?t2p1]
                [?t2 :team/player2 ?t2p2]
                [?t2 :team/score   _]] dbc league-id)
         (map (fn [[mid mt t1 t2 tx]]
                {:id mid :matchdate mt :tx tx
                 :team1 (merge {:id t1} (get-team t1 dbc))
                 :team2 (merge {:id t2} (get-team t2 dbc))
                 :reported-by (get-reported-by mid dbc)}))
         (sort-by (juxt :matchdate :tx)))))

(defprotocol FoosballDatabase
  (get-player-db [this id])
  (get-players-db [this])
  (get-matches-db [this])
  (rename-player-db [this id new-name])

  (create-player-db [this name openid])

  (create-match-db [this match])

  (activate-player-db [this id])
  (deactivate-player-db [this id])

  (delete-match-db [this id])

  (get-players-with-openids-db [this])
  (get-players-without-openids-db [this])
  (get-players-openids-db [this id])
  (add-openid-to-player-db [this playerid openid])

  (get-leagues-for-player-db [this player-id])
  (get-players-in-league-db [this league-id]))

(defrecord Database [db-uri connection]
  component/Lifecycle
  FoosballDatabase

  (start [component]
    (info "Starting Database")
    (let [conn (create-db-and-connect db-uri)]
      (info "Connected to database on uri: " db-uri)
      (assoc component :connection conn)))

  (stop [component]
    (info "Stopping Database")
    (assoc component :connection nil))

  (get-player-db [this id]
    (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] (db connection) id)
         ffirst))

  (get-players-db [this]
    (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pid :player/name ?n]
              [?pid :player/active ?a]
              [?pid :user/role ?role]] (db connection))
       (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
       (sort-by :name)))

  (create-player-db [this name openid]
    (let [playerid (d/tempid :db.part/user)
          result    @(d/transact connection
                                 [{:db/id playerid :player/name name}
                                  {:db/id playerid :player/active true}
                                  {:db/id playerid :user/openids openid}
                                  {:db/id playerid :user/role :user}])]
      (-> result :tempids first second)))

  (create-match-db [this {:keys [matchdate team1 team2 reported-by league-id]}]
    (let [[match-id team1-id team2-id] (repeatedly #(d/tempid :db.part/user))
          [t1p1 t1p2 t1score] (map team1 [:player1 :player2 :score])
          [t2p1 t2p2 t2score] (map team2 [:player1 :player2 :score])
          transaction [{:db/id team1-id :team/player1 t1p1}
                       {:db/id team1-id :team/player2 t1p2}
                       {:db/id team1-id :team/score t1score}

                       {:db/id team2-id :team/player1 t2p1}
                       {:db/id team2-id :team/player2 t2p2}
                       {:db/id team2-id :team/score t2score}

                       {:db/id match-id :match/team1 team1-id}
                       {:db/id match-id :match/team2 team2-id}
                       {:db/id match-id :match/time matchdate}
                       {:db/id match-id :match/reported-by reported-by}
                       {:db/id match-id :match/league league-id}]]
      (d/transact connection transaction)))

  (get-matches-db [this]
    (let [dbc (db connection)]
      (->> (d/q '[:find ?m ?mt ?t1 ?t2 ?tx
                  :in $
                  :where
                  [?m :match/time  ?mt ?tx]
                  [?m :match/team1 ?t1]
                  [?m :match/team2 ?t2]
                  [?t1 :team/player1 _]
                  [?t1 :team/player2 _]
                  [?t1 :team/score   _]
                  [?t2 :team/player1 _]
                  [?t2 :team/player2 _]
                  [?t2 :team/score   _]] dbc)
           (map (fn [[mid mt t1 t2 tx]]
                  {:id mid :matchdate mt :tx tx
                   :team1 (merge {:id t1} (get-team t1 dbc))
                   :team2 (merge {:id t2} (get-team t2 dbc))
                   :reported-by (get-reported-by mid dbc)}))
           (sort-by (juxt :matchdate :tx)))))

  (activate-player-db [this id]
    @(d/transact connection [{:db/id id :player/active true}]))

  (deactivate-player-db [this id]
    @(d/transact connection [{:db/id id :player/active false}]))

  (delete-match-db [this id]
    (let [matchdate (->> (d/q '[:find ?mt :in $ ?id
                                :where [?id :match/time ?mt]] (db connection) id)
                         ffirst)]
      (d/transact connection [[:db/retract id :match/time matchdate]])))

  (get-players-with-openids-db [this]
    (->> (d/q '[:find ?pid ?name
                :where
                [?pid :player/name ?name]
              [?pid :user/openids _]]
              (db connection))
         (map (fn [[id name]] (util/symbols-as-map id name)))))

  (get-players-without-openids-db [this]
    (let [playerids-with-openid (->> (get-players-with-openids-db this)
                                     (map (fn [{:keys [id]}] id))
                                     (set))]
      (->> (get-players)
           (filter (fn [{:keys [id]}] (not (playerids-with-openid id)))))))

  (get-players-openids-db [this id]
    (->> (d/q '[:find ?openids :in $ ?id :where [?id :user/openids ?openids]] (db connection) id)
         (mapcat identity)
         (set)))

  (add-openid-to-player-db [this playerid openid]
    @(d/transact connection [{:db/id playerid :user/openids openid}]))

  (get-leagues-for-player-db [this player-id]
    (->> (d/q '[:find ?lid ?name
                :in $ ?player-id
                :where
                [?player-id :player/leagues ?lid]
                [?lid :league/name ?name]] (db connection) player-id)
         (map (fn [[id name]] (util/symbols-as-map id name)))))

  (get-players-in-league-db [this league-id]
    (->> (d/q '[:find ?pid ?n ?a ?role
                :in $ ?league-id
                :where
                [?pid :player/name ?n]
                [?pid :player/leagues ?league-id]
                [?pid :player/active ?a]
                [?pid :user/role ?role]] (db connection) league-id)
         (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
         (sort-by :name)))
  )
