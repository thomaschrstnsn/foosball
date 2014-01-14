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

(defn get-all [dbc]
  (->> (d/q '[:find ?m ?mt
              ?t1id ?t1p1 ?t1p2 ?t1score
              ?t2id ?t2p1 ?t2p2 ?t2score
              ?tx
              :in $
              :where
              [?m :match/time  ?mt ?tx]
              [?m :match/team1 ?t1id]
              [?m :match/team2 ?t2id]

              [?t1id :team/player1 ?t1p1id]
              [?t1p1id :player/name ?t1p1]
              [?t1id :team/player2 ?t1p2id]
              [?t1p2id :player/name ?t1p2]
              [?t1id :team/score   ?t1score]

              [?t2id :team/player1 ?t2p1id]
              [?t2p1id :player/name ?t2p1]
              [?t2id :team/player2 ?t2p2id]
              [?t2p2id :player/name ?t2p2]
              [?t2id :team/score   ?t2score]] dbc)
       (map (fn [[mid mt
                 t1id t1p1 t1p2 t1score
                 t2id t2p1 t2p2 t2score
                 tx]]
              (let [match (d/entity dbc mid)
                    reporter (d/entity dbc (get-in match [:match/reported-by :db/id] ))]
                {:id mid :matchdate mt :tx tx
                 :team1 {:id t1id :player1 t1p1 :player2 t1p2 :score t1score}
                 :team2 {:id t2id :player1 t2p1 :player2 t2p2 :score t2score}
                 :reported-by (:player/name reporter)})))
       (sort-by (juxt :matchdate :tx))))

(defn delete! [conn id]
  (let [matchdate (->> (d/q '[:find ?mt :in $ ?id
                              :where [?id :match/time ?mt]] (db conn) id)
                       ffirst)] ;; this seems utterly ridiculous - must be a better way
    (d/transact conn [[:db/retract id :match/time matchdate]])))
