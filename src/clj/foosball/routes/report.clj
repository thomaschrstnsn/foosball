(ns foosball.routes.report
  (:require [cemerick.friend :as friend]
            [compojure.core :as compojure :refer [GET POST]]
            [foosball.auth :as auth]
            [foosball.models.domains :as d]
            [foosball.util :as util]
            [foosball.validation.match :as validation]
            [foosball.views.layout :as layout]
            [foosball.views.match :as match]
            [ring.util.response :as response]
            [taoensso.timbre :refer [info]]))

(def report-match-title "Report Match")

(defn report-match-page
  ([{:keys [config-options db]} ]
     (let [playerid (auth/current-auth :playerid)]
       (layout/common config-options
                      :title report-match-title
                      :content (match/form (d/get-players db)))))

  ([{:keys [config-options db]} team1player1 team1player2 team2player1 team2player2]
     (let [params   (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed   (match/parse-form params)
           playerid (auth/current-auth :playerid)]
       (layout/common config-options
                      :title report-match-title
                      :content (match/form (d/get-players db)
                                           parsed)))))

(defn report-match [{:keys [config-options db]} {:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        valid-report?    (->> validated-report vals ((partial every? identity)))
        reported-by      (auth/current-auth :playerid)]
    (if valid-report?
      (do
        (info (util/symbols-as-map parsed-form reported-by))
        (d/create-match! db (merge parsed-form (util/symbols-as-map reported-by)))
        (response/redirect-after-post "/stats/players"))
      (layout/common config-options
                     :title report-match-title
                     :content (match/form (d/get-players db) parsed-form)))))

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
                       (GET  "/match/with-players"
                             [t1p1 t1p2 t2p1 t2p2]
                             (report-match-page deps t1p1 t1p2 t2p1 t2p2))
                       (POST "/match"
                             request
                             (report-match deps request)))]
    (compojure/routes
     (compojure/context "/report" request (friend/wrap-authorize report-routes #{auth/user}))
     (GET "/matches"
          []
          (matches-page deps)))))
