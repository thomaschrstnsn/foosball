(ns foosball.locations.admin
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [chan <! put!]]
            [foosball.date :as d]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [foosball.spinners :refer [spinner]]))

(defn refresh-matches [app]
  (data/go-get-data! {:server-url "/api/matches"
                      :app app
                      :key :matches
                      :server-data-transform data/add-uuid-key
                      :set-to-nil-until-complete true}))

(defn handle [app v]
  (let [unauthorized? (and (:auth @app)
                           (not (-> @app :auth :admin?)))]
    (if-not unauthorized?
      (do
        (data/ensure-player-data app)
        (refresh-matches app)
        (loc/set-location app (:id v)))
      (.back js/history))))

(defn delete-match! [app match-id]
  (go
    (let [url  (str "/api/match/" match-id)
          __   (debug :delete-match! url)
          resp (<! (data/delete! url))]
      (when-not (= 204 (:status resp))
        (error :response resp)
        (js/alert "Something went wrong."))
      (refresh-matches app))))

(defcomponentk matches-component
  [[:data matches player-lookup :as app]
   owner]
  (init-state [_]
    {:delete-chan (chan)})

  (will-mount [_]
    (let [delete-chan (om/get-state owner [:delete-chan])]
      (go-loop []
        (let [to-delete (<! delete-chan)
              ok? (js/confirm "Deleting match. Are you sure?")]
          (when ok? (delete-match! app to-delete))
          (recur)))))

  (render-state [_ {:keys [delete-chan]}]
    (html
     (let [number-of-matches 50
           matches (->> matches (take-last number-of-matches) reverse)
           players-from-team (fn [{:keys [player1 player2]}] (mapv :id [player1 player2]))
           columns  [{:heading "Date played"
                      :fn      :matchdate
                      :printer d/->str
                      :align   :left}
                     {:heading "Team 1"
                      :key     :team1-players
                      :fn      (comp players-from-team :team1)
                      :align   :left
                      :printer (partial f/format-team-links player-lookup)}
                     {:heading "Score"
                      :key     :team1-score
                      :fn      (comp :score :team1)
                      :printer f/style-score}
                     {:heading "Team 2"
                      :key     :team2-players
                      :fn      (comp players-from-team :team2)
                      :align   :left
                      :printer (partial f/format-team-links player-lookup)}
                     {:heading "Score"
                      :key     :team2-score
                      :fn      (comp :score :team2)
                      :printer f/style-score}
                     {:heading "Reported by"
                      :key     :reported-by
                      :fn      (comp :id :reported-by)
                      :printer (partial f/format-player-link player-lookup)
                      :align   :left}
                     {:heading "Action"
                      :fn      :match/id
                      :align   :center
                      :printer (fn [id] [:button.btn.btn-danger
                                        {:on-click (fn [e] (put! delete-chan id))}
                                        "Delete!"])}]]
       (om/build table/table
                 {:rows          matches
                  :columns       columns
                  :caption       [:h1 (str "The " number-of-matches " most recent matches")]
                  :default-align :right
                  :class         ["table-hover" "table-bordered"]}
                 {:state {}})))))

(defn render [{:keys [matches auth player-lookup] :as data}]
  (if-not (and matches auth)
    (spinner)
    (om/build matches-component data)))
