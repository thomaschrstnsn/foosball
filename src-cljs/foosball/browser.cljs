(ns foosball.browser
  (:use [jayq.core :only [document-ready]])
  (:use-macros [secretary.macros :only [defroute]])
  (:require [foosball.dom-utils :as u]
            [foosball.navbar :as navbar]
            [foosball.player-log :as player-log]
            [foosball.matchup :as matchup]
            [foosball.report :as report]
            [clojure.browser.repl :as repl]
            [secretary.core :as secretary]))

(defroute "/player/log" {}
  (player-log/auto-submit-playerlog))

(defroute "/matchup" {}
  (matchup/enable-submit-on-enough-players))

(defroute "/report/match" {}
  (report/live-validation-of-match-report))

(defroute "/report/match/:league-id" {:as params}
  (report/live-validation-of-match-report))

(defn ^:export page-loaded []
  (navbar/set-active-navbar-element!)
  (secretary/dispatch! (u/current-path)))

(defn ^:export register-document-ready []
  (document-ready page-loaded))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
