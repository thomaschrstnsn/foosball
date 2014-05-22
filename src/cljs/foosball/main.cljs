(ns foosball.main
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [chan <!]]
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

(defmethod handle-new-location :location/player-statistics [app v]
  (om/update! app :player-statistics nil)
  (go (let [response (<! (http/get "/api/ratings/player-stats"))]
        (om/update! app :player-statistics (:body response))))
  (set-location app (:id v)))

(defmethod handle-new-location :location/team-statistics [app v]
  (om/update! app :team-statistics nil)
  (go (let [response (<! (http/get "/api/ratings/team-stats"))]
        (om/update! app :team-statistics (:body response))))
  (set-location app (:id v)))

(defmethod handle-new-location :default [app {:keys [id]}]
  (set-location app id))

(defmulti render-location (fn [{:keys [current-location]}] current-location))

(defmethod render-location :location/player-statistics [{:keys [current-location player-statistics]}]
  (let [columns [{:heading "Position"
                  :key :position
                  :sort-fn identity}
                 {:heading "Player"
                  :key :player
                  :printer (fn [r] [:a {:href "#/omg"} r])
                  :sort-fn identity}
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
                 {:heading ["Inactive" [:br] "Days/Matches"]
                  :key #(select-keys % [:days-since-latest-match :matches-after-last])
                  :printer (fn [{:keys [days-since-latest-match matches-after-last]}]
                             (list days-since-latest-match "/" matches-after-last))}
                 {:heading "Form"
                  :key :form
                  :printer f/style-form}
                 {:heading "Rating"
                  :key :rating
                  :printer f/style-rating
                  :sort-fn identity}]]
    (om/build table/table player-statistics {:opts {:columns      columns
                                                    :caption      [:h1 "Player Statistics"]
                                                    :default-sort {:key :position
                                                                   :dir :asc}}})))

(defmethod render-location :location/team-statistics [{:keys [current-location team-statistics]}]
  (let [columns [{:heading "Team"
                  :key :team}
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
                  :sort-fn identity}]]
    (om/build table/table team-statistics {:opts {:columns      columns
                                                  :caption      [:h1 "Team Statistics"]
                                                  :default-sort {:key :wins
                                                                 :dir :desc}}})))

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
