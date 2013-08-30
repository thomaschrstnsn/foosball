(ns foosball.models.db
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [datomic.api :only [q db] :as d])
  (:use [foosball.models.schema :only [eav-schema]])
  (:use [clojure.set :only [difference]]))

(def ^:dynamic ^:private conn)

(defn- ensure-players-have-active-flags []
  (let [dbc                      (db conn)
        players-with-names       (->> (d/q '[:find ?pid :where [?pid :player/name _]] dbc)
                                      (map (fn [[id]] id))
                                      set)
        players-with-active-flag (->> (d/q '[:find ?pid :where [?pid :player/active _]] dbc)
                                      (map (fn [[id]] id))
                                      set)
        players-to-default       (difference players-with-names players-with-active-flag)
        transaction              (map (fn [id] {:db/id id :player/active true}) players-to-default)]
    @(d/transact conn transaction)))

(def migrations
  "seq of idempotent migration functions"
  {"ensure-players-have-active-flags" ensure-players-have-active-flags})

(defn create-db-and-connect [uri]
  (info "creating database on uri:" uri)
  (d/create-database uri)

  (info "connecting to database")
  (alter-var-root #'conn (constantly (d/connect uri)))

  (info "transacting schema")
  @(d/transact conn eav-schema)

  (doseq [[name migration-fn] migrations]
    (info "running migration:" name)
    (migration-fn))

  (info "database initialized")
  conn)

(defn delete-db-and-disconnect [uri]
  (d/delete-database uri)
  (alter-var-root #'conn (constantly nil)))

(defn create-player [name]
  (let [player-id (d/tempid :db.part/user)]
    @(d/transact conn [{:db/id player-id :player/name name}
                       {:db/id player-id :player/active true}])))

(defn rename-player [id newplayername]
  @(d/transact conn [{:db/id id :player/name newplayername}]))

(defn activate-player [id]
  @(d/transact conn [{:db/id id :player/active true}]))

(defn deactivate-player [id]
  @(d/transact conn [{:db/id id :player/active false}]))

(defn get-players []
  (->> (d/q '[:find ?pid ?n ?a :where
              [?pid :player/name ?n]
              [?pid :player/active ?a]] (db conn))
       (map (fn [[id name active]] {:id id :name name :active active}))
       (sort-by :name)))

(defn create-match [{:keys [matchdate team1 team2]}]
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
                     {:db/id match-id :match/time matchdate}]]
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
                 :team2 (merge {:id t2} (get-team t2 dbc))}))
         (sort-by (juxt :matchdate :tx)))))
