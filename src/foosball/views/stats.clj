(ns foosball.views.stats
  (:use [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [foosball.statistics team-player ratings])
  (:use [foosball.util])
  (:require [clojure.string :as string]))

(defn- render-player [players p]
  [:tr
   [:td (->> p :player (get-player-by-name players) link-to-player-log)]
   [:td (:wins p)]
   [:td (:losses p)]
   [:td (:total p)]
   [:td (format-percentage (:win-perc p))]
   [:td (format-percentage (:loss-perc p))]
   [:td (:score-delta p)]
   [:td (format-rating (:rating p))]])

(defn- render-team [players t]
  [:tr
   [:td (->> (:team t)
             (map #(->> % (get-player-by-name players) link-to-player-log))
             (interpose ", "))]
   [:td (:wins t)]
   [:td (:losses t)]
   [:td (:total t)]
   [:td (format-percentage (:win-perc t))]
   [:td (format-percentage (:loss-perc t))]
   [:td (:score-delta t)]])

(defn- order-by [order seq]
  (if (= order :desc)
    (reverse seq)
    seq))

(defn- sortable-column [heading kw]
  (let [sort (str "sort=" (name kw))
        desc (str "?" sort "&order=desc")
        asc  (str "?" sort "&order=asc")]
    [:th heading
     [:span.pull-right
      [:a {:href desc} [:i.icon-chevron-up]]
      [:a {:href asc}  [:i.icon-chevron-down]]]]))

(defn- common-columns [first-column & last-column]
  [:tr
   first-column
   (sortable-column "Wins" :wins)
   (sortable-column "Losses" :losses)
   (sortable-column "Played" :total)
   (sortable-column "Wins %" :win-perc)
   (sortable-column "Losses %" :loss-perc)
   (sortable-column "Score diff." :score-delta)
   last-column])

(defn player-table [matches players & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Player Statistics"]]
    [:thead (common-columns (sortable-column "Player" :player)
                            (sortable-column "Rating" :rating))
     [:tbody
      (let [stats             (calculate-player-stats matches)
            ratings           (calculate-ratings matches)
            stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                     (merge stat {:rating (ratings player)}))
                                   stats)]
        (->> stats-and-ratings
             (sort-by (if (nil? sort) :rating sort))
             (order-by (if (nil? order) :desc order))
             (map (partial render-player players))))]]]))

(defn team-table [matches players & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Team Statistics"]]
    [:thead (common-columns [:th "Team"])
    [:tbody
     (->> matches
          calculate-team-stats
          (sort-by (if (nil? sort) :wins sort))
          (order-by (if (nil? order) :desc order))
          (map (partial render-team players)))]]]))
