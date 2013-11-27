(ns foosball.routes.report
  (:use [compojure.core :only [GET POST]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.validation.match :as validation]
            [foosball.auth :as auth]
            [foosball.util :as util]
            [foosball.models.domains :as d]
            [ring.util.response :as response]
            [cemerick.friend :as friend]
            [compojure.core :as compojure]))

(def report-match-title "Report Match")

(defn report-match-page
  ([{:keys [db]}]
     (let [playerid (auth/current-auth :playerid)
           leagues  (d/get-leagues-for-player db playerid)]
       (response/redirect (str "/report/match/" (->> leagues first :id)))))

  ([{:keys [config-options db]} league-id]
     (let [playerid (auth/current-auth :playerid)
           leagues  (d/get-leagues-for-player db playerid)
           league   (->> leagues
                         (filter (fn [{:keys [id]}] (= id league-id)))
                         first)]
       (layout/common config-options
                      :title report-match-title
                      :content (match/form (d/get-players-in-league db league-id)
                                           leagues
                                           league-id))))

  ([{:keys [config-options db]} league-id team1player1 team1player2 team2player1 team2player2]
     (let [params   (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed   (match/parse-form params)
           playerid (auth/current-auth :playerid)
           leagues  (d/get-leagues-for-player db playerid)]
       (layout/common config-options
                      :title report-match-title
                      :content (match/form (d/get-players db)
                                           leagues
                                           parsed)))))

(defn report-match [{:keys [config-options db]} {:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        league-id        (:league-id parsed-form)
        playerid         (auth/current-auth :playerid)
        leagues          (d/get-leagues-for-player db playerid)
        valid-league-id? (some (fn [{:keys [id]}] (= id league-id)) leagues)
        valid-report?    (->> validated-report vals ((partial every? identity)))
        reported-by      (auth/current-auth :playerid)]
    (if (and valid-report? valid-league-id?)
      (do
        (info (util/symbols-as-map parsed-form reported-by))
        (d/create-match! db (merge parsed-form (util/symbols-as-map reported-by)))
        (response/redirect-after-post "/stats/players"))
      (layout/common config-options
                     :title report-match-title
                     :content (match/form (d/get-players db) leagues parsed-form)))))

(defn matches-page [{:keys [config-options db]}]
  (layout/common config-options
                 :title "Matches"
                 :auto-refresh? true
                 :content (match/table (d/get-matches db)
                                       (d/get-players db))))

(defn routes [deps]
  (let [report-routes (compojure/routes
                       (GET  "/match"
                             []
                             (report-match-page deps))
                       (POST "/match/league-select"
                             [league-id]
                             (response/redirect-after-post (str "/report/match/" league-id)))
                       (GET  "/match/:league-id"
                             [league-id]
                             (report-match-page deps (util/parse-id league-id)))
                       (GET  "/match/:league-id/with-players"
                             [league-id t1p1 t1p2 t2p1 t2p2]
                             (report-match-page deps t1p1 t1p2 t2p1 t2p2))
                       (POST "/match/:league-id"
                             request
                             (report-match deps request)))]
    (compojure/routes
     (compojure/context "/report" request (friend/wrap-authorize report-routes #{auth/user}))
     (GET "/matches"
          []
          (matches-page deps)))))
