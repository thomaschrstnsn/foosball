(ns foosball.routes.report
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.validation.match :as validation]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn report-match-page
  ([] (layout/common (match/form (db/get-players))))
  ([team1player1 team1player2 team2player1 team2player2]
     (let [params (util/symbols-as-map team1player1 team1player2 team2player1 team2player2)
           parsed (match/parse-form params)]
       (layout/common (match/form (db/get-players) parsed)))))

(defn report-match [{:keys [params]}]
  (info {:report-match-params params})
  (let [parsed-form      (match/parse-form params)
        validated-report (validation/validate-report parsed-form)
        valid-report?    (->> validated-report vals ((partial every? identity)))]
    (if valid-report?
      (do
        (info {:validated-report validated-report})
        (db/create-match parsed-form)
        (redirect-after-post "/stats/players"))
      (layout/common (match/form (db/get-players) parsed-form)))))

(defn matches-page []
  (layout/common (match/table (db/get-matches) (db/get-players))))

(defroutes report-routes
  (GET "/report/match" [] (report-match-page))
  (GET "/report/match/with-players" [t1p1 t1p2 t2p1 t2p2] (report-match-page t1p1 t1p2 t2p1 t2p2))
  (POST "/report/match" request (report-match request))
  (GET "/matches" [] (matches-page)))
