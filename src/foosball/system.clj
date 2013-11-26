(ns foosball.system
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util      :as util]
            [foosball.app   :as app]
            [foosball.models.db :as db]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.nrepl.server :as nrepl]))


(defrecord HostedRepl [port server]
  component/Lifecycle

  (start [component]
    (info "Starting Hosted REPL")
    (let [repl-server (nrepl/start-server :port port)]
      (info "REPL running on port" port)
      (assoc component :server repl-server)))

  (stop [component]
    (info "Stopping Hosted REPL")
    (when server
      (nrepl/stop-server server))
    (assoc component :server nil)))

(defrecord WebServer [port server app handler-wrapper]
  component/Lifecycle

  (start [component]
    (info "Starting Web-server")
    (let [handler (:war-handler app)
          wrapped (handler-wrapper handler)
          server  (jetty/run-jetty wrapped {:port port :join? false})]
      (info "Web-server running on port:" port)
      (assoc component :server server)))

  (stop [component]
    (info "Stopping Web-server")
    (when server
      (.stop server))
    (assoc component :server nil)))

(def dev-system-components  [:db :app :web-server])
(def prod-system-components [:db :repl :app])

(defrecord Foosball [components config-options db repl web-server app]
  component/Lifecycle
  (start [this]
    (component/start-system this components))
  (stop [this]
    (component/stop-system this components)))

(def default-config-options
  {:db-uri          "datomic:free://localhost:4334/foosball"
   :cljs-optimized? true
   :repl-port       4321
   :web-port        8080
   :handler-wrapper identity})

(defn system [components & {:as config-overrides}]
  (let [config-options (merge default-config-options config-overrides)]
    (map->Foosball
     {:components     components
      :config-options config-options
      :db             (db/map->Database {:uri (:db-uri config-options)})
      :repl           (map->HostedRepl  {:port (:repl-port config-options)})
      :app            (component/using (app/map->App {})
                                       {:database       :db
                                        :config-options :config-options})
      :web-server     (component/using (map->WebServer
                                        {:port            (:web-port config-options)
                                         :handler-wrapper (:handler-wrapper config-options)})
                                       {:app :app})})))
