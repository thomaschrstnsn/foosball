(ns foosball.models.domains.matches
  (:require [datomic.api :as d :refer [db]]))

(defn create! [conn {:keys [matchdate team1 team2 reported-by]}]
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

(defn- get-player [dbc id]
  (->> (d/q '[:find ?player :in $ ?id :where [?id :player/name ?player]] dbc id)
       ffirst))

(defn- get-team [dbc id]
  (->> (d/q '[:find ?p1 ?p2 ?score
              :in $ ?id :where
              [?id :team/player1 ?p1]
              [?id :team/player2 ?p2]
              [?id :team/score ?score]] dbc id)
       (map (fn [[p1 p2 score]] {:player1 (get-player dbc p1)
                                :player2 (get-player dbc p2)
                                :score score}))
       first))

(defn- get-reported-by [dbc matchid]
  (->> (d/q '[:find ?n ?pid
              :in $ ?matchid
              :where
              [?matchid :match/reported-by ?pid]
              [?pid     :player/name       ?n]] dbc matchid)
       (map (fn [[name id]] name))
       first))

(defn get-all [dbc]
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
               :team1 (merge {:id t1} (get-team dbc t1))
               :team2 (merge {:id t2} (get-team dbc t2))
               :reported-by (get-reported-by dbc mid)}))
       (sort-by (juxt :matchdate :tx))))

(defn delete! [conn id]
  (let [matchdate (->> (d/q '[:find ?mt :in $ ?id
                              :where [?id :match/time ?mt]] (db conn) id)
                       ffirst)] ;; this seems utterly ridiculous - must be a better way
    (d/transact conn [[:db/retract id :match/time matchdate]])))
