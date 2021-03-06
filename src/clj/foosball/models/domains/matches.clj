(ns foosball.models.domains.matches
  (:require [clojure.set :refer [rename-keys]]
            [datomic.api :as d :refer [db]]
            [foosball.models.domains.helpers :as h]
            [foosball.util :as util]
            [foosball.entities :as e]
            [schema.core :as s]))

(defn create! [conn {:keys [matchdate team1 team2 reported-by id league-id]}]
  (let [[match-id team1-id team2-id] (repeatedly #(d/tempid :db.part/user))
        dbc                          (db conn)
        player-entity-from-id        (partial h/entity-id-from-attr-value dbc :player/id)
        league-entity                (h/entity-id-from-attr-value dbc :league/id league-id)
        team-fns                     [(comp player-entity-from-id :player1)
                                      (comp player-entity-from-id :player2)
                                      :score]
        apply-fn-to-map              (fn [m] (fn [f] (f m)))
        [t1p1 t1p2 t1score]          (map (apply-fn-to-map team1) team-fns)
        [t2p1 t2p2 t2score]          (map (apply-fn-to-map team2) team-fns)
        reporter-entity              (player-entity-from-id reported-by)
        existing-match-entity        (when id (h/ensure-id-is-unique dbc :match/id id))
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
                                      {:db/id match-id :match/id id}
                                      {:db/id match-id :match/league league-entity}]]
    @(d/transact conn transaction)))

(defn match-query [by-id?]
  (let [find     '[:find ?m ?mid ?mt
                   ?t1id ?t1score
                   ?t2id ?t2score
                   ?lid
                   ?tx]
        in       (if by-id?
                   '[:in $ ?id]
                   '[:in $])
        where    '[:where
                   [?m :match/time    ?mt ?tx]
                   [?m :match/id      ?mid]
                   [?m :match/team1   ?t1id]
                   [?m :match/team2   ?t2id]
                   [?m :match/league  ?lent]
                   [?lent :league/id  ?lid]
                   [?t1id :team/score ?t1score]
                   [?t2id :team/score ?t2score]]
        id-query (when by-id? '[[?m :match/id ?id]])]
    (vec (concat find in where id-query))))

(defn db-entity-to-player
  [ent]
  (-> ent
      (select-keys [:player/id :player/name])
      (rename-keys {:player/id   :id
                    :player/name :name})))

(defn map-match-query-results [dbc rs]
  (map (fn [[mid id mt t1id t1score t2id t2score leagueid tx]]
         (let [match    (d/entity dbc mid)
               reporter (->> (get-in match [:match/reported-by :db/id])
                             (d/entity dbc)
                             db-entity-to-player)
               team1    (d/entity dbc t1id)
               team2    (d/entity dbc t2id)
               [t1p1 t1p2
                t2p1 t2p2] (for [team      [team1 team2]
                                 player-kw [:team/player1 :team/player2]]
                             (-> (get-in team [player-kw :db/id])
                                 ((partial d/entity dbc))
                                 db-entity-to-player))]
           {:match/id id :matchdate mt :tx tx
            :team1 {:id t1id :player1 t1p1 :player2 t1p2 :score t1score}
            :team2 {:id t2id :player1 t2p1 :player2 t2p2 :score t2score}
            :reported-by reporter
            :league/id leagueid}))
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
