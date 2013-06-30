(ns user
  "nRepl development setup alá http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all set-refresh-dirs)]
            [ring.middleware.stacktrace :as stacktrace]
            [foosball.system :as system]
            [foosball.models.db :as db]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly (system/system :handler-wrapper stacktrace/wrap-stacktrace))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system system/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (system/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  true)

(defn reset []
  (stop)
  (set-refresh-dirs "src/" "dev/")
  (refresh :after 'user/go))
