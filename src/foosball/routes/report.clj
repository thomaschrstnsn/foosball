(ns foosball.routes.report
  (:use compojure.core hiccup.element ring.util.response)
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [foosball.views.layout :as layout]
            [foosball.views.match  :as match]
            [foosball.util :as util]
            [foosball.models.db :as db]))

(defn report-match-page []
  (layout/common (match/form (db/get-players))))

(defn report-match [{:keys [params]}]
  (info {:report-match-params params})
  (let [validated-report (->> params
                              match/parse-form
                              match/validate-report)]
    (if (->> validated-report :validation-errors empty?)
      (do
        (info {:validated-report validated-report})
        (db/create-match validated-report)
        (redirect-after-post "/stats/players"))
      (layout/common (match/form (db/get-players) validated-report)))))

(defn matches-page []
  (layout/common (match/table (db/get-matches) (db/get-players))))

(defroutes report-routes
  (GET "/" [] (redirect "/report/match"))

  (GET "/report/match" [] (report-match-page))
  (POST "/report/match" request (report-match request))

  (GET "/matches" [] (matches-page)))
