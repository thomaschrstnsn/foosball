(ns foosball.test.helpers
  (:require [com.stuartsierra.component :as c]
            [taoensso.timbre :as t]
            [foosball.models.db :as db]
            [foosball.app :as a]
            [foosball.system :as system]
            [foosball.models.domains :as d]
            [foosball.util :as util]
            [clojure.data :as data]
            [clojure.pprint :refer [pprint]]))

;;;; asserts/diffs

(defn diff-with-first-is-nil? [expected actual]
  (let [diff (first (data/diff expected actual))]
    (when diff (pprint {:diff diff}))
    (= nil (first (data/diff expected actual)))))

(defn seq-diff-with-first-is-nil? [expecteds actuals]
  (every? identity (map (fn [exp act] (diff-with-first-is-nil? exp act)) expecteds actuals)))

(defn roughly= [expected actual delta]
  (let [acceptable (and (number? actual)
                    (>= expected (- actual delta))
                    (<= expected (+ actual delta)))]
    (when-not acceptable (pprint {:expected expected
                                  :actual   actual
                                  :delta    delta}))
    acceptable))

;;;; util

(defn make-uuid []
  (java.util.UUID/randomUUID))

(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
     ~@tests))

(comment "example:"
         (with-private-fns [org.foo.bar [fn1 fn2]]
           (deftest test-fn1..)
           (deftest test-fn2..)))

;;;; db

(defn memory-db []
  (c/start
   (db/map->Database {:uri (str "datomic:mem://foosball-" (make-uuid))})))

;;;; fixture

(defn only-error-log-fixture [f]
  (t/with-log-level :error
    (f)))

;;;; app

(defn app-with-memory-db []
  (c/start (a/map->App {:database (memory-db)
                        :config-options system/default-config-options})))

;;;; dummy data generators

(defn create-dummy-player [db seed]
  (let [id (make-uuid)
        name (str "name-" seed)
        openid (str "openid-" seed)]
    (d/create-player! db id name openid )
    (util/identity-map id name openid)))

(defn create-dummy-league [db seed]
  (let [id (make-uuid)
        name (str "league-" seed)
        description (str "league-description-" seed)]
    (d/create-league! db id name description)
    (util/identity-map id name description)))
