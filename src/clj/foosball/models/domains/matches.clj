(ns foosball.models.domains.matches
  (:require [datomic.api :as d :refer [db]]
            [foosball.models.domains.helpers :as h]
            [foosball.util :as util]
            [foosball.entities :as e]
            [schema.core :as s]
            [taoensso.timbre :as t]))

(defn create! [conn {:keys [matchdate team1 team2 reported-by id]}]
  (let [[match-id team1-id team2-id] (repeatedly #(d/tempid :db.part/user))
        dbc                          (db conn)
        player-entity-from-id        (partial h/entity-id-from-attr-value dbc :player/id)
        team-fns                     [(comp player-entity-from-id :player1)
                                      (comp player-entity-from-id :player2)
                                      :score]
        apply-fn-to-map              (fn [m] (fn [f] (f m)))
        [t1p1 t1p2 t1score]          (map (apply-fn-to-map team1) team-fns)
        [t2p1 t2p2 t2score]          (map (apply-fn-to-map team2) team-fns)
        reporter-entity              (player-entity-from-id reported-by)
        id                           (or id (util/create-uuid))
        transaction                  [{:db/id team1-id :team/player1 t1p1}
                                      {:db/id team1-id :team/player2 t1p2}
                                      {:db/id team1-id :team/score t1score}

                                      {:db/id team2-id :team/player1 t2p1}
                                      {:db/id team2-id :team/player2 t2p2}
                                      {:db/id team2-id :team/score t2score}

                                      {:db/id match-id :match/team1 team1-id}
                                      {:db/id match-id :match/team2 team2-id}
                                      {:db/id match-id :match/time matchdate}
                                      {:db/id match-id :match/reported-by reporter-entity}
                                      {:db/id match-id :match/id id}]
        _ (t/info :transaction transaction)]
    @(d/transact conn transaction)))

(defn match-query [by-id?]
  (let [find     '[:find ?m ?mid ?mt
                   ?t1id ?t1p1 ?t1p2 ?t1score
                   ?t2id ?t2p1 ?t2p2 ?t2score
                   ?tx]
        in       (if by-id?
                   '[:in $ ?id]
                   '[:in $])
        where    '[:where
                   [?m :match/time  ?mt ?tx]
                   [?m :match/id    ?mid]
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
                   [?t2id :team/score   ?t2score]]
        id-query (when by-id? '[[?m :match/id ?id]])]
    (vec (concat find in where id-query))))

(defn map-match-query-results [dbc rs]
  (map (fn [[mid id mt
            t1id t1p1 t1p2 t1score
            t2id t2p1 t2p2 t2score
            tx]]
         (let [match (d/entity dbc mid)
               reporter (d/entity dbc (get-in match [:match/reported-by :db/id] ))]
           {:match/id id :matchdate mt :tx tx
            :team1 {:id t1id :player1 t1p1 :player2 t1p2 :score t1score}
            :team2 {:id t2id :player1 t2p1 :player2 t2p2 :score t2score}
            :reported-by (:player/name reporter)}))
       rs))

(defn chronologically-order-matches [ms]
  (sort-by (juxt :matchdate :tx) ms))

(s/defn get-all :- [e/Match]
  [dbc]
  (->> (d/q (match-query false) dbc)
       (map-match-query-results dbc)
       chronologically-order-matches))

(s/defn get-by-id :- (s/maybe e/Match)
  [dbc id]
  (->> (d/q (match-query true) dbc id)
       (map-match-query-results dbc)
       first))

(s/defn delete!
  [conn
   id :- s/Uuid]
  (let [entity-id (->> (d/q '[:find ?eid :in $ ?id
                              :where
                              [?eid :match/id ?id]]
                            (db conn) id)
                       ffirst)]
    (d/transact conn [[:db.fn/retractEntity entity-id]])))
