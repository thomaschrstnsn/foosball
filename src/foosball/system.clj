(ns foosball.system
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util      :as util]
            [foosball.handler   :as handler]
            [foosball.models.db :as db]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.nrepl.server :as nrepl]))

(def ^:private uri "datomic:free://localhost:4334/foosball")

(defn system
  "Returns a new instance of the whole application."
  [& {:keys [db-uri handler-wrapper] :or {db-uri uri handler-wrapper identity}}]
  {:db-uri db-uri
   :handler (handler-wrapper handler/war-handler)})

(defn start
  "Performs side effects to initialize the system, acquire resources, and start it running.
   Returns an updated instance of the system."
  [{:keys [handler db-uri] :as system}]
  (let [dev-port      8080
        repl-port     4321
        db-connection (db/create-db-and-connect db-uri)
        server        (when handler
                        (jetty/run-jetty handler {:port dev-port :join? false}))
        repl-host     (nrepl/start-server :port repl-port)]
    (when server (info "dev-server running on port" dev-port))
    (when repl-host (info "nrepl host running on port" repl-port))
    (merge system (util/symbols-as-map db-connection server repl-host))))

(defn stop
  "Performs side effects to shut down the system and release its resources.
   Returns an updated instance of the system."
  [{:keys [server repl-host] :as system}]
  (when server
    (.stop server))
  (when repl-host
    (nrepl/stop-server repl-host))
  (merge system {:server nil
                 :db-connection nil
                 :repl-host nil}))
