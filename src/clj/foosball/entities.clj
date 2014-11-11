(ns foosball.entities
  (:require [schema.core :as s]))

(def Player {:id   s/Uuid
             :name s/Str})

(def User (merge Player
                 {:active s/Bool
                  :role   (s/enum :user :admin)}))

(def Team {:id      s/Int
           :player1 Player
           :player2 Player
           :score   s/Int})

(def Match {:match/id            s/Uuid
            :matchdate           s/Inst
            (s/optional-key :tx) s/Int
            :team1               Team
            :team2               Team
            :reported-by         Player
            :league/id           s/Uuid})
