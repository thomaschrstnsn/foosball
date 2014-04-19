(ns foosball.models.domains.helpers
  (:require [datomic.api :as d]))

(defn entity-id-from-attr-value [dbc attr value]
  (->> (d/q '[:find ?eid :in $ ?attr ?value :where [?eid ?attr ?value]] dbc attr value)
       ffirst))
