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

(defroute "/matchup" {}
  (navbar/set-active-navbar-by-id "#nav-matchup")
  (matchup/enable-submit-on-enough-players))

(defn- report-match-action []
  (navbar/set-active-navbar-by-id "#nav-report")
  (report/live-validation-of-match-report))

(defroute "/report/match" {}
  (report-match-action))

(defroute "/stats/players" {}
  (navbar/set-active-navbar-by-id "#nav-players-stats"))

(defroute "/stats/teams" {}
  (navbar/set-active-navbar-by-id "#nav-teams-stats"))

(defroute "/matches" {}
  (navbar/set-active-navbar-by-id "#nav-matches"))

(defroute "/player/log" {}
  (navbar/set-active-navbar-by-id "#nav-player-log")
  (player-log/auto-submit-playerlog))

(defroute "/admin" {}
  (navbar/set-active-navbar-by-id "#nav-admin"))

(defroute "/about" {}
  (navbar/set-active-navbar-by-id "#nav-about"))

(defn ^:export page-loaded []
  (secretary/dispatch! (u/current-path)))

(defn ^:export register-document-ready []
  (document-ready page-loaded))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
