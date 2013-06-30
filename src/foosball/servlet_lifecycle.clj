(ns foosball.servlet-lifecycle
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.system :as system]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (info "instantiating system")
  (def ^:private system-inst
    (-> (system/system)
        ;; override war handler - we are running inside a servlet, it will run it
        (merge  {:war-handler nil})
        (system/start)))
  (info "foosball started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (info "foosball shutting down...")
  (system/stop system-inst)
  (info "shut down ok"))
