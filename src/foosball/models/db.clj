(ns foosball.models.db
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [datomic.api :only [q db] :as d])
  (:use [foosball.models.schema :only [eav-schema]])
  (:use [clojure.set :only [difference]])
  (:require [foosball.util :as util]
            [foosball.models.migration :as migration]))

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

(defn activate-player [id]
  @(d/transact conn [{:db/id id :player/active true}]))

(defn deactivate-player [id]
  @(d/transact conn [{:db/id id :player/active false}]))

(defn set-players-role [id role]
  @(d/transact conn [{:db/id id :user/role role}]))

(defn get-players []
  (->> (d/q '[:find ?pid ?n ?a ?role :where
              [?pid :player/name ?n]
              [?pid :player/active ?a]
              [?pid :user/role ?role]] (db conn))
       (map (fn [[id name active role]] (util/symbols-as-map id name active role)))
       (sort-by :name)))

;; openid

(defn add-openid-to-player [playerid openid]
  @(d/transact conn [{:db/id playerid :user/openids openid}]))

(defn remove-openids-from-player [playerid openids]
  @(d/transact conn (vec (for [openid openids]
                           [:db/retract playerid :user/openids openid]))))

(defn get-players-openids [id]
  (->> (d/q '[:find ?openids :in $ ?id :where [?id :user/openids ?openids]] (db conn) id)
       (mapcat identity)
       (set)))

(defn remove-players-openids [id]
  (remove-openids-from-player id (get-players-openids id)))

(defn get-players-with-openids []
  (->> (d/q '[:find ?pid ?name
              :where
              [?pid :player/name ?name]
              [?pid :user/openids _]]
            (db conn))
       (map (fn [[id name]] (util/symbols-as-map id name)))))

(defn get-players-without-openids []
  (let [playerids-with-openid (->> (get-players-with-openids)
                                   (map (fn [{:keys [id]}] id))
                                   (set))]
    (->> (get-players)
         (filter (fn [{:keys [id]}] (not (playerids-with-openid id)))))))

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

(defn create-match [{:keys [matchdate team1 team2 reported-by]}]
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
                     {:db/id match-id :match/reported-by reported-by}]]
    (d/transact conn transaction)))

(defn delete-match [id]
  (let [matchdate (->> (d/q '[:find ?mt :in $ ?id :where [?id :match/time ?mt]] (db conn) id)
                       ffirst)]
    (d/transact conn [[:db/retract id :match/time matchdate]])))

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
