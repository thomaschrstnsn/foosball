(ns foosball.locations.report-match
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn handle [app v]
  (if (-> @app :auth :logged-in?)
    (do
      (when-not (@app :players)
        (data/go-update-data! "/api/players" app :players))
      (loc/set-location app (:id v)))
    (.back js/history)))

(defn report-match-component [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:h1 "HELLO COMPONENT"]))))

(defn render [app]
  (om/build report-match-component app))
