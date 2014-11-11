(ns foosball.models.domains.helpers
  (:require [datomic.api :as d]))

(defn entity-id-from-attr-value [dbc attr value]
  (->> (d/q '[:find ?eid :in $ ?attr ?value :where [?eid ?attr ?value]] dbc attr value)
       ffirst))

(defn ensure-id-is-unique [dbc id-attr id]
  (when (entity-id-from-attr-value dbc id-attr id)
    (throw (Exception. (str id-attr " is not unique")))))
