(ns foosball.system
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.handler   :as handler]
            [foosball.models.db :as db]
            [ring.adapter.jetty :as jetty]))

(def ^:private uri "datomic:free://localhost:4334/foosball")

(defn system
  "Returns a new instance of the whole application."
  [& {:keys [db-uri handler-wrapper] :or {db-uri uri handler-wrapper identity}}]
  {:db-uri uri
   :handler (handler-wrapper handler/war-handler)})

(defn start
  "Performs side effects to initialize the system, acquire resources, and start it running.
   Returns an updated instance of the system."
  [{:keys [handler db-uri] :as system}]
  (let [dev-port      8080
        db-connection (db/create-db-and-connect db-uri)
        server        (when handler
                        (jetty/run-jetty handler {:port dev-port :join? false}))]
    (when server (info "dev-server running on port" dev-port))
    (merge system {:db-connection db-connection
                   :server server})))

(defn stop
  "Performs side effects to shut down the system and release its resources.
   Returns an updated instance of the system."
  [{:keys [server] :as system}]
  (when server
    (.stop server))
  (merge system {:server nil
                 :db-connection nil}))
