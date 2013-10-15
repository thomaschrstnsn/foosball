(ns foosball.routes.report
  (:use [compojure.core :only [defroutes context GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [ring.util.response :as response]
            [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.validation.match :as validation]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [cemerick.friend :as friend]))

(def report-match-title "Report Match")

(defn report-match-page
  ([] (let [playerid (auth/current-auth :playerid)
            leagues  (db/get-leagues-for-player playerid)]
        (report-match-page (->> leagues first :id))))

  ([league-id]
     (let [playerid (auth/current-auth :playerid)
           leagues  (db/get-leagues-for-player playerid)
           league   (->> leagues
                         (filter (fn [{:keys [id]}] (= id league-id)))
                         first)]
       (layout/common report-match-title
                      (match/form (db/get-players-in-league league-id) leagues league-id))))

  ([league-id team1player1 team1player2 team2player1 team2player2]
     (let [params (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed (match/parse-form params)]
       (layout/common report-match-title (match/form (db/get-players) parsed)))))

(defn report-match [{:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        valid-report?    (->> validated-report vals ((partial every? identity)))
        reported-by      (auth/current-auth :playerid)]
    (if valid-report?
      (do
        (info (util/symbols-as-map parsed-form reported-by))
        (db/create-match (merge parsed-form (util/symbols-as-map reported-by)))
        (response/redirect-after-post "/stats/players"))
      (layout/common report-match-title (match/form (db/get-players) parsed-form)))))

(defn matches-page []
  (layout/common "Matches" (match/table (db/get-matches) (db/get-players))))

(defroutes report-routes
  (GET  "/match" [] (report-match-page))
  (POST "/match/league-select" [league-id] (response/redirect-after-post (str "/report/match/" league-id)))
  (GET  "/match/:league-id" [league-id] (report-match-page (util/parse-id league-id)))
  (GET  "/match/:league-id/with-players" [league-id t1p1 t1p2 t2p1 t2p2] (report-match-page t1p1 t1p2 t2p1 t2p2))
  (POST "/match" request (report-match request)))

(defroutes routes
  (context "/report" request (friend/wrap-authorize report-routes #{auth/user}))
  (GET  "/matches" [] (matches-page)))
