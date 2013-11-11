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
            [foosball.models.db :as db]
            [dev-data :as d]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders (socket :as socket-appender)]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly (system/system
                               :handler-wrapper stacktrace/wrap-stacktrace
                               :cljs-optimized? false))))

(defn socket-logger []
  (timbre/set-config! [:appenders :socket] socket-appender/socket-appender)
  (timbre/set-config! [:shared-appender-config :socket]
                      {:listen-addr :all
                       :port 9000}))

(defn start
  "Starts the current development system."
  []
  (socket-logger)
  (alter-var-root #'system system/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (system/stop s))))
  :ok)

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  :ok)

(defn reset []
  (stop)
  (set-refresh-dirs "src/" "dev/")
  (refresh :after 'user/go))

(defn cljs-repl-set! [repl]
  (def repl-env (reset! cemerick.austin.repls/browser-repl-env repl)))

(defn cljs-repl-setup
  "Setup the app to use a Austin browser hosted cljs-repl"
  []
  (cljs-repl-set! (cemerick.austin/repl-env)))

(defn cljs-repl-connect
  "Connect to the browser hosted cljs-repl"
  []
  (cemerick.austin.repls/cljs-repl repl-env))

(defn delete-database-and-stop []
  (db/delete-db-and-disconnect (:db-uri system))
  (stop)
  :ok)
