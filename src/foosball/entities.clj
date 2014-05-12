(ns foosball.entities
  (:require [schema.core :as s]))

(def Player {(s/required-key :id)     s/Uuid
             (s/required-key :name)   s/Str
             (s/required-key :active) s/Bool
             (s/required-key :role)   (s/enum :user :admin)})
