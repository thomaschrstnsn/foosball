(ns foosball.servlet-lifecycle
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.system            :as system]
            [noir.util.middleware       :as middleware]
            [com.stuartsierra.component :as component]))

(def ^:private system nil)

(def handler nil)

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (info "instantiating system")
  (alter-var-root #'system  (constantly (-> (system/system system/prod-system-components)
                                            component/start)))
  (alter-var-root #'handler (constantly (middleware/war-handler (get-in system [:app :ring-handler]))))
  (info "foosball started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (info "foosball shutting down...")
  (alter-var-root #'system (fn [s] (when s (component/stop s))))
  (info "shut down ok"))
