(ns user
  (:require [figwheel.client :as fw :include-macros true]
            [foosball.main :as foos]
            [foosball.console :refer-macros [debug error]]))

(foos/init-app)

(fw/watch-and-reload
 :websocket-url   "ws://localhost:3449/figwheel-ws"
 :jsload-callback (fn []
                    (debug "figwheel reload")
                    (try (foos/init-app)
                         (catch :default e (error "giving up!" e)))))
