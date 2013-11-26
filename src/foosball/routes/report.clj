(ns foosball.routes.report
  (:use [compojure.core :only [GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.validation.match :as validation]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.db :as db]
            [ring.util.response :as response]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(def report-match-title "Report Match")

(defn report-match-page
  ([db] (let [playerid (auth/current-auth :playerid)
              leagues  (db/get-leagues-for-player-db db playerid)]
          (response/redirect (str "/report/match/" (->> leagues first :id)))))

  ([db league-id]
     (let [playerid (auth/current-auth :playerid)
           leagues  (db/get-leagues-for-player-db db playerid)
           league   (->> leagues
                         (filter (fn [{:keys [id]}] (= id league-id)))
                         first)]
       (layout/common :title report-match-title
                      :content (match/form (db/get-players-in-league-db db league-id)
                                           leagues
                                           league-id))))

  ([db league-id team1player1 team1player2 team2player1 team2player2]
     (let [params (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed (match/parse-form params)]
       (layout/common :title report-match-title
                      :content (match/form (db/get-players-db db) parsed)))))

(defn report-match [db {:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        league-id        (:league-id parsed-form)
        playerid         (auth/current-auth :playerid)
        leagues          (db/get-leagues-for-player-db db playerid)
        valid-league-id? (some (fn [{:keys [id]}] (= id league-id)) leagues)
        valid-report?    (->> validated-report vals ((partial every? identity)))
        reported-by      (auth/current-auth :playerid)]
    (if (and valid-report? valid-league-id?)
      (do
        (info (util/symbols-as-map parsed-form reported-by))
        (db/create-match-db db (merge parsed-form (util/symbols-as-map reported-by)))
        (response/redirect-after-post "/stats/players"))
      (layout/common :title report-match-title
                     :content (match/form (db/get-players-db db) parsed-form)))))

(defn matches-page [db]
  (layout/common :title "Matches"
                 :auto-refresh? true
                 :content (match/table (db/get-matches-db db)
                                       (db/get-players-db db))))

(defn routes [db]
  (let [report-routes (compojure/routes
                       (GET  "/match"
                             []
                             (report-match-page db))
                       (POST "/match/league-select"
                             [league-id]
                             (response/redirect-after-post (str "/report/match/" league-id)))
                       (GET  "/match/:league-id"
                             [league-id]
                             (report-match-page db (util/parse-id league-id)))
                       (GET  "/match/:league-id/with-players"
                             [league-id t1p1 t1p2 t2p1 t2p2]
                             (report-match-page db t1p1 t1p2 t2p1 t2p2))
                       (POST "/match/:league-id"
                             request
                             (report-match db request)))]
    (compojure/routes
     (compojure/context "/report" request (friend/wrap-authorize report-routes #{auth/user}))
     (GET "/matches"
          []
          (matches-page db)))))
