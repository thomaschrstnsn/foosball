(ns foosball.locations.matchup
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [foosball.macros :refer [identity-map]])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.routes :as routes]
            [foosball.spinners :refer [spinner]]
            [foosball.console :refer-macros [debug debug-js info log trace error]])
  (:import goog.array))

(defn update-matchups [app playerids]
  (let [query-params (map-indexed (fn [idx playerid] (str "playerid" idx "=" playerid)) playerids)
        query        (str/join "&" query-params)]
    (data/go-get-data! {:server-url (str "/api/matchup?" query)
                        :app app
                        :key :matchups
                        :set-to-nil-until-complete true})
    (om/update! app :matchups-requested true)))

(defn handle [app v]
  (let [unauthorized? (and (:auth @app)
                           (not (-> @app :auth :logged-in?)))]
    (if-not unauthorized?
      (do
        (data/ensure-player-data app)
        (om/update! app :matchups nil)
        (om/update! app :matchup-selected-playerids nil)
        (om/update! app :matchups-requested false)
        (loc/set-location app (:id v)))
      (.back js/history))))

(defn- format-matchup-percentage [p]
  (f/style-value p
                :printer (partial f/format-percentage 3)
                :class? #(not= (double 0) (double %))))

(defn- headers-matchup []
  [:thead
   [:tr
    [:th [:div.text-right "Team 1"]]
    [:th [:div.text-center "Rating ±"]]
    [:th ""]
    [:th [:div.text-center "Expected %"]]
    [:th ""]
    [:th [:div.text-center "Rating ±"]]
    [:th "Team 2"]
    [:th ""]]])

(defn- render-match-report-button [team1 team2]
  (let [[t1p1 t1p2] (seq team1)
        [t2p1 t2p2] (seq team2)]
    [:a.btn.btn-default {:href (routes/report-match-path {:query-params (identity-map t1p1 t1p2 t2p1 t2p2)})}
     "Report result"]))

(defn- render-matchup
  [player-lookup {:keys [pos-players neg-players expected-diff pos-rating-diff neg-rating-diff]}]
  [:tr
   [:td [:div.text-right  (f/format-team-links player-lookup pos-players)]]
   [:td [:div.text-center (f/style-rating pos-rating-diff)]]
   [:td [:div.text-center (when (pos? expected-diff) [:span.glyphicon.glyphicon-arrow-left])]]
   [:td [:div.text-center (format-matchup-percentage (* 100 expected-diff))]]
   [:td [:div.text-center (when (neg? expected-diff) [:span.glyphicon.glyphicon-arrow-right])]]
   [:td [:div.text-center (f/style-rating neg-rating-diff)]]
   [:td (f/format-team-links player-lookup neg-players)]
   [:td (render-match-report-button pos-players neg-players)]])

(defcomponentk player-selection-comp
  [[:data players matchup-selected-playerids :as app]
   owner
   [:opts selection-change-ch :as opts]]
  (render-state [_ state]
    (if-let [active-players  (filter :active players)]
      (html
       [:div.form-horizontal
        [:div.control-group
         [:label.control-label.col-lg-2 {:for "playerids"} "Select atleast four players"]
         [:div.col-lg-2
          (let [number-of-players-to-show (min 16 (count active-players))]
            [:select.form-control {:multiple "multiple"
                                   :size number-of-players-to-show
                                   :on-change (fn [e]
                                                (let [selection (-> e .-target .-selectedOptions
                                                                    goog.array/toArray (.map (fn [o] (.-value o)))
                                                                    js->clj)]
                                                  (put! selection-change-ch selection)))}
             (->> active-players
                  (map (fn [{:keys [id name]}]
                         [:option (merge {:value id})
                          name])))])]]
        (let [enough-players? (<= 4 (count matchup-selected-playerids))]
          [:div.control-group.col-lg-3
           [:button.btn.btn-primary.btn-lg.btn-block
            (merge
             (when-not enough-players? {:disabled "disabled"})
             {:on-click (fn [e]
                          (update-matchups app @matchup-selected-playerids)
                          false)})
            "Show possible matchups"]])]))))

(defcomponentk matchup-component
  [[:data players player-lookup matchups-requested
    {matchups nil} {matchup-selected-playerids nil} :as app]
   owner]
  (init-state [_]
    {:selection-change-ch (chan)})

  (will-mount [_]
    (let [selection-change-ch (om/get-state owner :selection-change-ch)]
      (go-loop []
        (let [[v c] (alts! [selection-change-ch])]
          (condp = c
            selection-change-ch (om/update! app :matchup-selected-playerids v)
            nil)
          (recur)))))

  (render-state [_ {:keys [selection-change-ch]}]
    (html
     [:div
      [:h1 "Design the perfect matchup"]
      [:p.lead
       "Pick the players available for a match (atleast four)." [:br]
       "Then see the possible combinations of teams and their expected win/lose ratios."]
      (om/build player-selection-comp app {:opts {:selection-change-ch selection-change-ch}})
      (when matchups-requested
        (if-not (and player-lookup matchups)
          (spinner)
          [:table.table.table-hover
           [:caption [:h1 "Matchups"]]
           (headers-matchup)
           [:tbody
            (->> matchups
                 (sort-by :expected-sortable)
                 (map (partial render-matchup player-lookup)))]]))])))

(defn render [{:keys [players player-lookup] :as app}]
  (if-not (and players player-lookup)
    (spinner)
    (om/build matchup-component app)))
