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
            [foosball.models.domains.openids :as openid]
            [dev-data :as d]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders (socket :as socket-appender)]
            [org.httpkit.client :as http]
            [clojure.edn :as edn]
            [com.stuartsierra.component :as component]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (constantly (system/system system/dev-system-components
                                        :handler-wrapper stacktrace/wrap-stacktrace
                                        :cljs-optimized? false
                                        :repl-port 12345))))

(defn socket-logger []
  (timbre/set-config! [:appenders :socket] socket-appender/socket-appender)
  (timbre/set-config! [:shared-appender-config :socket]
                      {:listen-addr :all :port 9000}))

(defn start
  "Starts the current development system."
  []
  (socket-logger)
  (alter-var-root #'system component/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s))))
  :ok)

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start)
  :ok)

(defn reset-and-then [after]
  (stop)
  (set-refresh-dirs "src/clj" "dev/" "test/clj")
  (refresh :after after))

(defn run-tests
  "Runs tests against the codebase."
  []
  (clojure.test/run-all-tests))

(defn reload-and-run-clojure-tests
  "Reloads and runs the clojure.test tests loaded"
  []
  (reset-and-then 'clojure.test/run-all-tests))

(defn reset []
  (reset-and-then 'user/go))

(defn delete-database-and-stop! []
  (db/delete! (:db system))
  (stop)
  :ok)

(defn conn []
  (get-in system [:db :connection]))

(defn db []
  (datomic.api/db (conn)))

(defn http-get
  ([path] (http-get path {}))
  ([path opts]
     @(http/get (str "http://localhost:" (get-in system [:config-options :web-port]) path)
                (merge {:as :text} opts))))

(defn response-as-edn [resp]
  (update-in resp [:body] edn/read-string))

(defn leaderboard [n]
  (-> (str "/api/ratings/leaderboard/" n) http-get response-as-edn :body pprint))
