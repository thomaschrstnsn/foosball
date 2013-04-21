(ns foosball.views.stats
  (:use [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:require [clojure.set :as set])
  (:require [clojure.string :as string]))

(defn- determine-winner [{:keys [team1 team2] :as match}]
  (let [t1score  (:score team1)
        t2score  (:score team2)
        t1delta  (- t1score t2score)
        t2delta  (- t2score t1score)
        [t1 t2]  (map (fn [{:keys [player1 player2]}] #{player1 player2}) [team1 team2])
        [winners
         losers] (if (> t1score t2score)
                   [t1 t2]
                   [t2 t1])]
    (-> match
        (assoc-in [:winners] winners)
        (assoc-in [:losers]  losers)
        (assoc-in [:score-delta] [[t1 t1delta]
                                  [t2 t2delta]]))))

(defn- players-from-matches [matches]
  (->> matches
       (map (fn [m] [(:winners m) (:losers m)]))
       flatten
       (apply set/union)))

(defn- teams-from-matches [matches]
  (->> matches
       (reduce (fn [acc m] (conj acc (:winners m) (:losers m))) [])
       set))

(defn- player-stats [matches player]
  (let [wins        (->> (map :winners matches) (filter #(contains? % player)) count)
        losses      (->> (map :losers  matches) (filter #(contains? % player)) count)
        total       (+ wins losses)
        win-perc    (* (/ wins total) 100)
        loss-perc   (* (/ losses total) 100)
        score-delta (->>  (map :score-delta matches) (reduce concat)
                          (filter (fn [[t s]] (contains? t player))) (map (fn [[t s]] s))
                          (reduce + 0))]
    {:player player :wins wins :losses losses :total total
     :win-perc win-perc :loss-perc loss-perc :score-delta score-delta}))

(defn- team-stats [matches team]
  (let [wins      (->> (map :winners matches) (filter (partial = team)) count)
        losses    (->> (map :losers  matches) (filter (partial = team)) count)
        total     (+ wins losses)
        win-perc  (* (/ wins total) 100)
        loss-perc (* (/ losses total) 100)
        score-delta (->>  (map :score-delta matches) (reduce concat)
                          (filter (fn [[t s]] (= team t))) (map (fn [[t s]] s))
                          (reduce + 0))]
    {:team team :wins wins :losses losses :total total :win-perc win-perc :loss-perc loss-perc :score-delta score-delta}))

(defn- calculate-player-stats [matches]
  (let [won-matches (map determine-winner matches)
        players     (players-from-matches won-matches)]
    (map (partial player-stats won-matches) players)))

(defn- calculate-team-stats [matches]
  (let [won-matches (map determine-winner matches)
        teams       (teams-from-matches won-matches)]
    (map (partial team-stats won-matches) teams)))

(defn- format-percentage [p]
  (format "%.1f%%" (double p)))

(defn- render-player [p]
  [:tr
   [:td (:player p)]
   [:td (:wins p)]
   [:td (:losses p)]
   [:td (:total p)]
   [:td (format-percentage (:win-perc p))]
   [:td (format-percentage (:loss-perc p))]
   [:td (:score-delta p)]])

(defn- render-team [t]
  [:tr
   [:td (string/join ", " (:team t))]
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

(defn- common-columns [first-column]
  [:tr
   first-column
   (sortable-column "Wins" :wins)
   (sortable-column "Losses" :losses)
   (sortable-column "Played" :total)
   (sortable-column "Wins %" :win-perc)
   (sortable-column "Losses %" :loss-perc)
   (sortable-column "Score diff." :score-delta)])

(defn player-table [matches & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Player Statistics"]]
    [:thead (common-columns (sortable-column "Player" :player))
     [:tbody
      (->> matches
           calculate-player-stats
           (sort-by (if (nil? sort) :wins sort))
           (order-by order)
           (map render-player))]]]))

(defn team-table [matches & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Team Statistics"]]
    [:thead (common-columns [:th "Team"])
    [:tbody
     (->> matches
          calculate-team-stats
          (sort-by (if (nil? sort) :wins sort))
          (order-by order)
          (map render-team))]]]))
