(ns foosball.models.schema
  (:require [datomic.api :as d]))

(def eav-schema
  [ ;; players (and users)
   {:db/id (d/tempid :db.part/db)
    :db/ident :player/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A player's id"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :player/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
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

   ;; team
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

   ;; match
   {:db/id (d/tempid :db.part/db)
    :db/ident :match/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A match's id"
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
    :db/doc "Date when match was played"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :match/reported-by
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user who reported the match result"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :match/league
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The league to which the match belongs"
    :db.install/_attribute :db.part/db}

   ;; leagues
   {:db/id (d/tempid :db.part/db)
    :db/ident :league/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A league's id"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :league/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A league's name"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :league/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A longer text describing the league"
    :db.install/_attribute :db.part/db}

   {:db/id (d/tempid :db.part/db)
    :db/ident :league/admins
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/unique :db.unique/identity
    :db/doc "A league's admins"
    :db.install/_attribute :db.part/db}])
