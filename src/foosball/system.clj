(ns foosball.system
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util      :as util]
            [foosball.app   :as app]
            [foosball.settings  :as settings]
            [foosball.models.db :as db]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :as nrepl]))


(defrecord HostedRepl [port server]
  component/Lifecycle

  (start [component]
    (info "Starting Hosted REPL")
    (let [repl-server (nrepl/start-server :port port)]
      (if repl-server
        (info "REPL running on port " port)
        (error "Could not start REPL"))
      (assoc component :server repl-server)))

  (stop [component]
    (info "Stopping Hosted REPL")
    (when server
      (nrepl/stop-server server))
    (assoc component :server nil)))

(defrecord WebServer [port server app]
  component/Lifecycle

  (start [component]
    (info "Starting Web-server")
    (let [handler (:war-handler app)
          server  (jetty/run-jetty handler {:port port :join? false})]
      (info "Web-server running on port:" port)
      (assoc component :server server)))

  (stop [component]
    (info "Stopping Web-server")
    (when server
      (.stop server))
    (assoc component :server nil)))

(def dev-system-components [:db :repl :web-server :app])

(defrecord FoosballSystem [config-options db repl web-server app]
  component/Lifecycle
  (start [this]
    (component/start-system this dev-system-components))
  (stop [this]
    (component/stop-system this dev-system-components)))

(defn foosball-system []
  (map->FoosballSystem
   {:config-options {}
    :db   (db/map->Database {:db-uri settings/default-datomic-uri})
    :repl (map->HostedRepl  {:port 4321})
    :app  (component/using
           (app/map->App {})
           {:database :db})
    :web-server (component/using
                 (map->WebServer {:port 8080})
                 {:app :app})}))

(comment
  (defn system
    "Returns a new instance of the whole application."
    [& {:keys [db-uri handler-wrapper cljs-optimized?] :or {db-uri settings/default-datomic-uri
                                                            handler-wrapper identity
                                                            cljs-optimized? true}}]
    {:db-uri db-uri
     :handler (handler-wrapper handler/war-handler)
     :cljs-optimized? cljs-optimized?})

  (defn start
    "Performs side effects to initialize the system, acquire resources, and start it running.
   Returns an updated instance of the system."
    [{:keys [handler db-uri cljs-optimized?] :as system}]
    (let [dev-port      8080
          repl-port     4321
          db-connection (db/create-db-and-connect db-uri)
          server        (when handler
                          (jetty/run-jetty handler {:port dev-port :join? false}))
          repl-host     (nrepl/start-server :port repl-port)]
      (when server (info "dev-server running on port:" dev-port))
      (when repl-host (info "nrepl host running on port:" repl-port))
      (alter-var-root #'settings/cljs-optimized? (constantly (true? cljs-optimized?)))
      (info "assuming cljs-optimization:" settings/cljs-optimized?)
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
                   :repl-host nil})))
