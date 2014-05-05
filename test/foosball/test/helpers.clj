(ns foosball.test.helpers
  (:require [com.stuartsierra.component :as c]
            [taoensso.timbre :as t]
            [foosball.models.db :as db]
            [clojure.data :as data]
            [clojure.pprint :refer [pprint]]))

;;;; asserts/diffs

(defn diff-with-first-nil? [expected actual]
  (let [diff (first (data/diff expected actual))]
    (when diff (pprint {:diff diff}))
    (= nil (first (data/diff expected actual)))))

;;;; util

(defn make-uuid []
  (java.util.UUID/randomUUID))

;;;; db

(defn memory-db []
  (c/start
   (db/map->Database {:uri (str "datomic:mem://foosball-" (make-uuid))})))

;;;; fixture

(defn only-error-log-fixture [f]
  (t/with-log-level :error
    (f)))