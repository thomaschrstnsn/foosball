(ns foosball.locations.matchup
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [clojure.string :as str]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [foosball.data :as data]
            [foosball.table :as table]
            [foosball.format :as f]
            [foosball.location :as loc]
            [foosball.console :refer-macros [debug debug-js info log trace error]])
  (:import goog.array))

(defn update-matchups [app playerids]
  (let [query-params (map-indexed (fn [idx playerid] (str "playerid" idx "=" playerid)) playerids)
        query        (str/join "&" query-params)]
    (data/go-update-data! (str "/api/matchup?" query) app :matchups)))

(defn handle [app v]
  (if (-> @app :auth :logged-in?)
    (do
      (when-not (@app :players)
        (data/go-update-data! "/api/players" app :players))
      (om/update! app :matchups nil)
      (om/update! app :matchup-selected-playerids nil)
      (loc/set-location app (:id v)))
    (.back js/history)))

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

(defn- render-match-report-button [players team1 team2]
  (let [;get-player-id-fn (fn [p] (->> p (get-player-by-name players) :id))
        ;[t1p1 t1p2]      (map get-player-id-fn team1)
        ;[t2p1 t2p2]      (map get-player-id-fn team2)
        ]
    [:a.btn.btn-default {;:href
                         #_ (str "/report/match/with-players"
                                    "?t1p1=" t1p1
                                    "&t1p2=" t1p2
                                    "&t2p1=" t2p1
                                    "&t2p2=" t2p2)
                         :on-click (fn [e] (error "not implemented yet!"))}
     "Report result"]))

(defn- render-matchup [players {:keys [pos-players neg-players expected-diff pos-rating-diff neg-rating-diff]}]
  [:tr
   [:td [:div.text-right  (f/format-team-links players pos-players)]]
   [:td [:div.text-center (f/style-rating pos-rating-diff)]]
   [:td [:div.text-center (when (pos? expected-diff) [:span.glyphicon.glyphicon-arrow-left])]]
   [:td [:div.text-center (format-matchup-percentage (* 100 expected-diff))]]
   [:td [:div.text-center (when (neg? expected-diff) [:span.glyphicon.glyphicon-arrow-right])]]
   [:td [:div.text-center (f/style-rating neg-rating-diff)]]
   [:td (f/format-team-links players neg-players)]
   [:td (render-match-report-button players pos-players neg-players)]])

(defn player-selection-comp [{:keys [players matchup-selected-playerids] :as app}
                             owner
                             {:keys [selection-change-ch] :as opts}]
  (reify
    om/IRenderState
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
              "Show possible matchups"]])])))))

(defn matchup-component [{:keys [players matchups matchup-selected-playerids] :as app} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:selection-change-ch (chan)})

    om/IWillMount
    (will-mount [_]
      (let [selection-change-ch (om/get-state owner :selection-change-ch)]
        (go-loop []
          (let [[v c] (alts! [selection-change-ch])]
            (condp = c
              selection-change-ch (om/update! app :matchup-selected-playerids v)
              nil)
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [selection-change-ch]}]
      (html
       [:div
        [:h1 "Design the perfect matchup"]
        [:p.lead
         "Pick the players available for a match (atleast four)." [:br]
         "Then see the possible combinations of teams and their expected win/lose ratios."]
        (om/build player-selection-comp app {:opts {:selection-change-ch selection-change-ch}})
        (when matchups
          (let [active-players (filter :active players)]
            [:table.table.table-hover
             [:caption [:h1 "Matchups"]]
             (headers-matchup)
             [:tbody
              (->> matchups
                   (sort-by :expected-sortable)
                   (map (partial render-matchup active-players)))]]))]))))

(defn render [app]
  (om/build matchup-component app))
