(ns foosball.location
  (:require [om.core :as om :include-macros true]))

(defn set-location [app id]
  (om/update! app :current-location id))
