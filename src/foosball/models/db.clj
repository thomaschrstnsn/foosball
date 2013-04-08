(ns foosball.models.db
  (:use [datomic.api :only [q db] :as d]))

(def uri "datomic:free://localhost:4334/foosball")

(def conn (d/connect uri))

(defn create-player [name]
  (d/transact conn [{:db/id (d/tempid :db.part/user)
                     :player/name name}]))

(defn delete-player [id]
  (let [name (-> (d/q '[:find ?n :in $ ?id :where [?id :player/name ?n]] (db conn) id)
                  first first)]
    (d/transact conn [[:db/retract id :player/name name]])))

(defn get-players []
  (->> (d/q '[:find ?c ?n :where [?c player/name ?n]] (db conn))
       (map (fn [[id name]] {:id id :name name}))))

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

(defn get-player
  ([id] (get-player id (db conn)))
  ([id dbc]
     (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] dbc id)
          first first)))

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
    (->> (d/q '[:find ?m ?mt ?t1 ?t2
                :in $
                :where
                [?m :match/time  ?mt]
                [?m :match/team1 ?t1]
                [?m :match/team2 ?t2]
                [?t1 :team/player1 _]
                [?t1 :team/player2 _]
                [?t1 :team/score   _]
                [?t2 :team/player1 _]
                [?t2 :team/player2 _]
                [?t2 :team/score   _]] dbc)
         (map (fn [[mid mt t1 t2]] {:id mid :matchdate mt
                                   :team1 (merge {:id t1} (get-team t1 dbc))
                                   :team2 (merge {:id t2} (get-team t2 dbc))})))))
