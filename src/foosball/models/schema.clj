(ns foosball.models.schema
  (:use [datomic.api :only [db] :as d]))

(def eav-schema
  [{:db/id (d/tempid :db.part/db)
    :db/ident :player/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A player's name"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :user/openids
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "A user's openid(s)"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :user/role
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "A user's role"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :player/active
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "A player's active flag"
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
    :db/doc "When did the match take place?"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :match/reported-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Which user/player reported the match?"
    :db.install/_attribute :db.part/db}])
