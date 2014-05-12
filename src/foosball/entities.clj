(ns foosball.entities
  (:require [schema.core :as s]))

(def Player {(s/required-key :id)     s/Uuid
             (s/required-key :name)   s/Str
             (s/required-key :active) s/Bool
             (s/required-key :role)   (s/enum :user :admin)})

(def Team {(s/required-key :id) s/Int
           (s/required-key :player1) s/Str
           (s/required-key :player2) s/Str
           (s/required-key :score) s/Int})

(def Match {(s/required-key :match/id) s/Uuid
            (s/required-key :matchdate) s/Inst
            (s/optional-key :tx) s/Int
            (s/required-key :team1) Team
            (s/required-key :team2) Team
            (s/required-key :reported-by) s/Str})
