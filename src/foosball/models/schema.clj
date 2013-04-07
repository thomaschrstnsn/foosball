(ns foosball.models.schema
  (:use [datomic.api :only [db] :as d])
  (:use [foosball.models.db :only [conn uri]]))

(defn- make-db []
  (d/create-database uri))

(defn- delete-db []
  (d/delete-database uri))

(defn- setup-attributes []
  (d/transact conn [{:db/id (d/tempid :db.part/db)
                     :db/ident :player/name
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity
                     :db/doc "A player's name"
                     :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :team/player1
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one
                      :db/doc "A team's first player"
                      :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :team/player2
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one
                      :db/doc "A team's second player"
                      :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :match/team1
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many
                      :db/doc "A match's first team"
                      :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :match/team2
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many
                      :db/doc "A match's second team"
                      :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :team/score
                      :db/valueType :db.type/long
                      :db/cardinality :db.cardinality/many
                      :db/doc "A team's scores"
                      :db.install/_attribute :db.part/db}

                     {:db/id (d/tempid :db.part/db)
                      :db/ident :match/time
                      :db/valueType :db.type/instant
                      :db/cardinality :db.cardinality/one
                      :db/doc "When did the match take place"
                      :db.install/_attribute :db.part/db}]))

(defn initialize []
  (make-db)
  (setup-attributes))
