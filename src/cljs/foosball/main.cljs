(ns foosball.main
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [chan <!]]
            [cljs-uuid-utils :as uuid]
            [foosball.routes :as routes]
            [foosball.menu :as menu]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defmulti request-new-location (fn [app req] (-> @app :current-location)))

(defmulti handle-new-location (fn [app req] (:id req)))

(defmethod request-new-location :default [app new]
  (handle-new-location app new))

(defn set-location [app id]
  (om/update! app :current-location id))

(defn go-update-data [url app key]
  (go (let [response (<! (http/get url))]
        (om/update! app key (:body response)))))

(defmethod handle-new-location :location/player-statistics [app v]
  (om/update! app :player-statistics nil)
  (when-not (@app :players)
    (go-update-data "/api/players" app :players))
  (go-update-data "/api/ratings/player-stats" app :player-statistics)
  (set-location app (:id v)))

(defmethod handle-new-location :location/team-statistics [app v]
  (om/update! app :team-statistics nil)
  (when-not (@app :players)
    (go-update-data "/api/players" app :players))
  (go-update-data "/api/ratings/team-stats" app :team-statistics)
  (set-location app (:id v)))

(defmethod handle-new-location :location/home [app v]
  (om/update! app :leaderboard nil)
  (when-not (@app :players)
    (go-update-data "/api/players" app :players))
  (go-update-data "/api/ratings/leaderboard/5" app :leaderboard)
  (set-location app (:id v)))

(defmethod handle-new-location :location/player-log [app {:keys [args] :as v}]
  (let [player-id (first args)]
    (when-not (@app :players)
      (go-update-data "/api/players" app :players))
    (om/update! app :player-log-player ())
    (om/update! app :players nil)
    (when player-id
      (do
        (om/update! app :player-log-player (uuid/make-uuid-from player-id))
        (go-update-data (str "/api/ratings/log/" player-id) app :player-log)))
    (go-update-data "/api/players" app :players)
    (set-location app (:id v))))

(defmethod handle-new-location :location/matches [app v]
  (om/update! app :matches nil)
  (when-not (@app :players)
    (go-update-data "/api/players" app :players))
  (go-update-data "/api/matches" app :matches)
  (set-location app (:id v)))

(defmethod handle-new-location :default [app {:keys [id]}]
  (set-location app id))

(defmulti render-location (fn [{:keys [current-location]}] current-location))

(defn- nav-button [url label]
  [:div.col-lg-6
      [:a.btn.btn-info.btn-lg.btn-block {:href url} label]])

(defmethod render-location :location/home [{:keys [leaderboard players]}]
  (when players
    (list
     [:div.jumbotron
      [:h1 "Foosball"]
      [:h2 "Keeps track of results, ratings and players for foosball matches."]
      (if false
        [:div
         [:div.row
          (nav-button (routes/player-statistics-path) "See ratings for all players")
          (nav-button "/matchup"       "Matchup players for a match")]
         [:br]
         [:div.row
          (nav-button "/report/match"  "Report the result of a match")
          (nav-button "/matches"       "See results of all played  matches")]]
        [:div.row
                                        ;       [:div.col-lg-6 (auth/login-form :button-class "btn-lg btn-block" :button-text "Login or create a new player")]
         (nav-button (routes/player-statistics-path) "See ratings for all players")])]
     (let [columns [{:heading "Position"
                     :key :position
                     :printer (fn [p] (str p "."))}
                    {:heading "Player"
                     :key :player/name
                     :printer (partial f/format-player-link players)
                     :align :left}
                    {:heading "Form"
                     :key :form
                     :printer (partial f/style-form :won :lost)
                     :align :left}
                    {:heading "Rating"
                     :key :rating
                     :printer f/style-rating}]]
       (om/build table/table leaderboard {:opts {:columns       columns
                                                 :caption       [:h1 "Current leaderboard"]
                                                 :default-container :h3
                                                 :class         ["table-hover" "table-condensed"]}})))))

(defmethod render-location :location/player-statistics [{:keys [player-statistics players]}]
  (when players
    (let [position-col {:heading "Position"
                        :key :position
                        :printer (fn [p] (str p "."))
                        :sort-fn identity}
          columns [position-col
                   {:heading "Player"
                    :key :player
                    :printer (partial f/format-player-link players)
                    :sort-fn identity
                    :align :left}
                   {:heading "Wins"
                    :key :wins
                    :sort-fn identity}
                   {:heading "Losses"
                    :key :losses
                    :sort-fn identity}
                   {:heading "Played"
                    :key :total
                    :sort-fn identity}
                   {:heading "Wins %"
                    :key :win-perc
                    :printer (partial f/style-match-percentage true)
                    :sort-fn identity}
                   {:heading "Losses %"
                    :key :loss-perc
                    :printer (partial f/style-match-percentage false)
                    :sort-fn identity}
                   {:heading "Score diff."
                    :key :score-delta
                    :printer f/style-value
                    :sort-fn identity}
                   {:heading [:div "Inactive" [:br] "Days/Matches"]
                    :key #(select-keys % [:days-since-latest-match :matches-after-last])
                    :printer (fn [{:keys [days-since-latest-match matches-after-last]}]
                               (list days-since-latest-match "/" matches-after-last))
                    :align :left}
                   {:heading "Form"
                    :key :form
                    :printer (partial f/style-form true false)
                    :align :left}
                   {:heading "Rating"
                    :key :rating
                    :printer f/style-rating
                    :sort-fn identity}]]
      (om/build table/table player-statistics {:opts {:columns       columns
                                                      :caption       [:h1 "Player Statistics"]
                                                      :default-align :right
                                                      :class         ["table-hover" "table-bordered"]}
                                               :state {:sort {:column position-col
                                                              :dir    :asc}}}))))

(defmethod render-location :location/team-statistics [{:keys [team-statistics players]}]
  (when players
    (let [wins-col {:heading "Wins"
                    :key :wins
                    :sort-fn identity}
          columns  [{:heading "Team"
                     :key :team
                     :align :left
                     :printer (partial f/format-team-links players)}
                    wins-col
                    {:heading "Losses"
                     :key :losses
                     :sort-fn identity}
                    {:heading "Played"
                     :key :total
                     :sort-fn identity}
                    {:heading "Wins %"
                     :key :win-perc
                     :printer (partial f/style-match-percentage true)
                     :sort-fn identity}
                    {:heading "Losses %"
                     :key :loss-perc
                     :printer (partial f/style-match-percentage false)
                     :sort-fn identity}
                    {:heading "Score diff."
                     :key :score-delta
                     :printer f/style-value
                     :sort-fn identity}]]
      (om/build table/table team-statistics {:opts {:columns       columns
                                                    :caption       [:h1 "Team Statistics"]
                                                    :default-align :right
                                                    :class         ["table-hover" "table-bordered"]}
                                             :state {:sort {:column wins-col
                                                            :dir    :desc}}}))))

(defmethod render-location :location/player-log [{:keys [player-log player-log-player players]}]
  (when players
    (let [player (when (and player-log-player player-log)
                   (->> players
                        (filter (fn [{:keys [id]}] (= id player-log-player)))
                        first))]
      (list
       [:div.form-group.col-lg-3
        [:select.form-control {:value (if player
                                        (str (:id player))
                                        "default")
                               :on-change (fn [e]
                                            (routes/navigate-to
                                             (routes/player-log-path {:player-id (-> e .-target .-value)})))}
         [:option {:value "default" :disabled "disabled"} "Active players"]
         (map (fn [{:keys [id name]}] [:option {:value id} name]) players)]]
       (when player-log
         (let [columns      [{:heading "Match date"
                              :key     :matchdate
                              :printer (fn [d] (when d (f/format-date d)))}
                             {:heading "Team mate"
                              :key     :team-mate
                              :printer (fn [tm] (when tm (f/format-player-link players tm)))}
                             {:heading "Opponents"
                              :key     :opponents
                              :printer (fn [ops] (when ops (f/format-team-links players ops)))}
                             {:heading "Expected"
                              :key     :expected
                              :printer (fn [v] (when v (f/style-match-percentage true (* 100 v))))}
                             {:heading "Actual"
                              :key     :win?}
                             {:heading "Inactive matches"
                              :key     :inactivity}
                             {:heading "Diff rating"
                              :key     :delta
                              :printer (fn [v] (when v (f/style-value v :printer f/format-rating)))}
                             {:heading "New rating"
                              :key     :new-rating
                              :printer f/style-rating}]
               row-class-fn (fn [{:keys [log-type]}] (when (= :inactivity log-type) "danger"))]
           (om/build table/table player-log {:opts {:columns       columns
                                                    :caption       [:h1 (str "Player Log: " (:name player))]
                                                    :default-align :right
                                                    :class         ["table-hover" "table-bordered"]
                                                    :row-class-fn  row-class-fn}})))))))

(defmethod render-location :location/matches [{:keys [matches players]}]
  (when players
    (let [players-from-team (fn [{:keys [player1 player2]}] [player1 player2])
          date-column {:heading "Date played"
                       :key :matchdate
                       :printer f/format-date
                       :align :left
                       :sort-fn identity}
          columns  [date-column
                    {:heading "Team 1"
                     :key (comp players-from-team :team1)
                     :align :left
                     :printer (partial f/format-team-links players)}
                    {:heading "Score"
                     :key (comp :score :team1)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Team 1"
                     :key (comp players-from-team :team2)
                     :align :left
                     :printer (partial f/format-team-links players)}
                    {:heading "Score"
                     :key (comp :score :team2)
                     :sort-fn identity
                     :printer f/style-score}
                    {:heading "Reported by"
                     :key :reported-by
                     :align :left}]]
      (om/build table/table matches {:opts {:columns       columns
                                            :caption       [:h1 "Played Matches"]
                                            :default-align :right
                                            :class         ["table-hover" "table-bordered"]}
                                             :state {:sort {:column date-column
                                                            :dir    :desc}}}))))

(defmethod render-location :default [{:keys [current-location]}]
  (list
   [:h1 (str current-location)]
   [:p  "Med dig"]))

(defn app-root [app owner {:keys [menu] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {})

    om/IWillMount
    (will-mount [_]
      (let [req-location-chan (om/get-state owner :req-location-chan)]
        (go-loop []
          (let [[v c] (alts! [req-location-chan])]
            (condp = c
              req-location-chan (request-new-location app v)
              nil)
            (recur)))))

    om/IRender
    (render [_]
      (html [:div (om/build menu/menu-bar app {:opts menu})
             [:div.container
              (render-location app)]]))))

(debug "initializing application")
(let [app (atom {})
      route-setup (routes/init!)]
  (om/root app-root app {:target     (. js/document (getElementById "app"))
                         :init-state {:req-location-chan (:req-location-chan route-setup)}
                         :opts       {:menu (select-keys route-setup [:home-location :menu-locations])}}))
