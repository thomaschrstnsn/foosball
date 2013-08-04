(ns foosball.browser
  (:use [jayq.core :only [document-ready]])
  (:require [foosball.dom-utils :as u]
            [foosball.navbar :as navbar]
            [foosball.player-log :as player-log]
            [foosball.matchup :as matchup]
            [foosball.report :as report]
            [clojure.browser.repl :as repl]))

(def page-fns {"/player/log"   player-log/auto-submit-playerlog
               "/matchup"      matchup/enable-submit-on-enough-players
               "/report/match" report/live-validation-of-match-report})

(defn ^:export page-loaded []
  ;(repl/connect "http://localhost:9000/repl")
  (let [auto-fn (get page-fns (u/current-path) nil)]
    (navbar/set-active-navbar-element!)
    (when auto-fn (auto-fn))))

(defn ^:export register-document-ready []
  (document-ready page-loaded))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
