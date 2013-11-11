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
        (response/redirect (str "/report/match/" (->> leagues first :id)))))

  ([league-id]
     (let [playerid (auth/current-auth :playerid)
           leagues  (db/get-leagues-for-player playerid)
           league   (->> leagues
                         (filter (fn [{:keys [id]}] (= id league-id)))
                         first)]
       (layout/common :title report-match-title
                      :content (match/form (db/get-players-in-league league-id) leagues league-id))))

  ([league-id team1player1 team1player2 team2player1 team2player2]
     (let [params (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed (match/parse-form params)]
       (layout/common :title report-match-title
                      :content (match/form (db/get-players) parsed)))))

(defn report-match [{:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        league-id        (:league-id parsed-form)
        playerid         (auth/current-auth :playerid)
        leagues          (db/get-leagues-for-player playerid)
        valid-league-id? (some (fn [{:keys [id]}] (= id league-id)) leagues)
        valid-report?    (->> validated-report vals ((partial every? identity)))
        reported-by      (auth/current-auth :playerid)]
    (if (and valid-report? valid-league-id?)
      (do
        (info (util/symbols-as-map parsed-form reported-by))
        (db/create-match (merge parsed-form (util/symbols-as-map reported-by)))
        (response/redirect-after-post "/stats/players"))
      (layout/common :title report-match-title
                     :content (match/form (db/get-players) parsed-form)))))

(defn matches-page []
  (layout/common :title "Matches"
                 :auto-refresh? true
                 :content (match/table (db/get-matches) (db/get-players))))

(defroutes report-routes
  (GET  "/match" [] (report-match-page))
  (POST "/match/league-select" [league-id] (response/redirect-after-post (str "/report/match/" league-id)))
  (GET  "/match/:league-id" [league-id] (report-match-page (util/parse-id league-id)))
  (GET  "/match/:league-id/with-players" [league-id t1p1 t1p2 t2p1 t2p2] (report-match-page t1p1 t1p2 t2p1 t2p2))
  (POST "/match/:league-id" request (report-match request)))

(defroutes routes
  (context "/report" request (friend/wrap-authorize report-routes #{auth/user}))
  (GET  "/matches" [] (matches-page)))
