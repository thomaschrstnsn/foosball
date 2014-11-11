(ns foosball.models.domains.leagues
  (:require [datomic.api :as d]
            [foosball.models.domains.helpers :as h]
            [foosball.util :as util]))

(defn create! [conn id name description]
  (let [eid (d/tempid :db.part/user)]
    @(d/transact conn
                 [{:db/id eid :league/id id}
                  {:db/id eid :league/name name}
                  {:db/id eid :league/description description}])))
