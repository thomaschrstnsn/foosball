(ns foosball.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [foosball.models.schema :as schema]))

(defdb db schema/db-spec)

(defentity players)

(defn create-player [player]
  (insert players
          (values player)))

(defn delete-player [id]
  (delete players (where {:id id})))

(defn get-players []
  (select players))

(defn get-user [id]
  (first (select players
                 (where {:id id})
                 (limit 1))))
