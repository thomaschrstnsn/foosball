(ns foosball.models.db
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d :refer [db]]
            [foosball.models.domains :as ==>]
            [foosball.models.domains.matches :as matches]
            [foosball.models.domains.openids :as openids]
            [foosball.models.domains.players :as players]
            [foosball.models.domains.leagues :as leagues]
            [foosball.models.migration :as migration]
            [foosball.models.schema :refer [eav-schema]]
            [taoensso.timbre :refer [info]]))

(defn- create-db-and-connect [uri]
  (info "Creating database on uri:" uri)
  (d/create-database uri)

  (info "Connecting to database")

  (let [connection (d/connect uri)]
    (info "Transacting schema")
    @(d/transact connection eav-schema)
    (migration/migrate-schema-and-data connection)
    connection))

(defprotocol Deletable
  (delete! [this]))

(defrecord Database [uri connection]
  component/Lifecycle
  (start [this]
    (info "Starting Database")
    (let [conn (create-db-and-connect uri)]
      (info "Connected to database on uri:" uri)
      (assoc this :connection conn)))

  (stop [this]
    (info "Stopping Database")
    (when connection (d/release connection))
    (assoc this :connection nil))

  Deletable
  (delete! [this]
    (info "Deleting database:" uri)
    (d/delete-database uri))

  ==>/Players
  (get-player [this id]
    (players/get-by-id (db connection) id))
  (get-players [this]
    (players/get-all (db connection)))

  (create-player! [this id name openid]
    (players/create! connection id name openid))
  (rename-player! [this id newplayername]
    (players/rename! connection id newplayername))

  (activate-player! [this id]
    (players/activate! connection id))
  (deactivate-player! [this id]
    (players/deactivate! connection id))

  ==>/Matches
  (get-matches [this]
    (matches/get-all (db connection)))
  (get-match [this id]
    (matches/get-by-id (db connection) id))
  (create-match! [this {:keys [matchdate team1 team2 reported-by] :as match}]
    (matches/create! connection match))
  (delete-match! [this id]
    (matches/delete! connection id))

  ==>/OpenIds
  (get-players-with-openids [this]
    (openids/get-all-with-openid (db connection)))
  (get-players-without-openids [this]
    (openids/get-all-without-openid (db connection)))
  (get-player-openids [this id]
    (openids/get-player-openids (db connection) id))
  (add-openid-to-player! [this playerid openid]
    (openids/add-openid-to-player! connection playerid openid))
  (get-player-with-given-openid [this openid]
    (openids/get-player-with-given-openid (db connection) openid))

  ==>/Leagues
  (create-league! [this id name description]
    (leagues/create! connection id name description)))
