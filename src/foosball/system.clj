(ns foosball.system
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.util       :as util]
            [foosball.app        :as app]
            [foosball.models.db  :as db]
            [foosball.repl       :as repl]
            [foosball.web-server :as web]
            [com.stuartsierra.component :as component]))

(def dev-system-components  [:db :repl :app :web-server])
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
      :db             (db/map->Database
                       {:uri (:db-uri config-options)})
      :repl           (repl/map->HostedRepl
                       {:port (:repl-port config-options)})
      :app            (component/using
                       (app/map->App {})
                       {:database       :db
                        :config-options :config-options})
      :web-server     (component/using
                       (web/map->WebServer
                        {:port            (:web-port config-options)
                         :handler-wrapper (:handler-wrapper config-options)})
                       {:app :app})})))
