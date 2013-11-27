(ns foosball.repl
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [com.stuartsierra.component :as component]
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
