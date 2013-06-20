(ns foosball.views.stats
  (:use [hiccup.page :only [html5]])
  (:use [taoensso.timbre :only [trace debug info warn error fatal spy]])
  (:use [foosball.statistics team-player ratings])
  (:use [foosball.util]))

(defn- format-match-percentage [p wins?]
  (format-value p
                :printer format-percentage
                :class? #(not= (double 50) (double %))
                :checker (if wins?
                           (partial < 50)
                           (partial > 50))))

(defn- render-player [players p]
  [:tr
   [:td (->> p :player (get-player-by-name players) link-to-player-log)]
   [:td (:wins p)]
   [:td (:losses p)]
   [:td (:total p)]
   [:td (format-match-percentage (:win-perc p)  true)]
   [:td (format-match-percentage (:loss-perc p) false)]
   [:td (format-value (:score-delta p))]
   [:td (map #(format-value % :printer {true "W" false "L"} :class? nil :checker true? :container-tag :span) (:form p))]
   [:td (format-value (:rating p) :printer format-rating :class? nil :checker (partial < 1500))]])

(defn- render-team [players t]
  [:tr
   [:td (->> (:team t)
             (map #(->> % (get-player-by-name players) link-to-player-log))
             (interpose ", "))]
   [:td (:wins t)]
   [:td (:losses t)]
   [:td (:total t)]
   [:td (format-match-percentage (:win-perc  t) true)]
   [:td (format-match-percentage (:loss-perc t) false)]
   [:td (format-value (:score-delta t))]])

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

(defn- common-columns [first-column & last-columns]
  [:tr
   first-column
   (sortable-column "Wins" :wins)
   (sortable-column "Losses" :losses)
   (sortable-column "Played" :total)
   (sortable-column "Wins %" :win-perc)
   (sortable-column "Losses %" :loss-perc)
   (sortable-column "Score diff." :score-delta)
   last-columns])

(defn player-table [matches players & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   (auto-refresh-page)
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Player Statistics"]]
    [:thead (common-columns (sortable-column "Player" :player)
                            [:th "Form"]
                            (sortable-column "Rating" :rating))
     [:tbody
      (let [stats             (calculate-player-stats matches)
            log-and-ratings   (ratings-with-log matches)
            ratings           (:ratings log-and-ratings)
            logs              (:logs log-and-ratings)
            stats-and-ratings (map (fn [{:keys [player] :as stat}]
                                     (merge stat
                                            {:rating (ratings player)}
                                            {:form   (calculate-current-form-for-player logs 5 player)}))
                                   stats)]
        (->> stats-and-ratings
             (sort-by (if (nil? sort) :rating sort))
             (order-by (if (nil? order) :desc order))
             (map (partial render-player players))))]]]))



(defn team-table [matches players & {:keys [sort order] :or {sort :wins order :desc}}]
  (html5
   (auto-refresh-page)
   [:table.table.table-hover.table-bordered
    [:caption [:h1 "Team Statistics"]]
    [:thead (common-columns [:th "Team"])
    [:tbody
     (->> matches
          calculate-team-stats
          (sort-by (if (nil? sort) :wins sort))
          (order-by (if (nil? order) :desc order))
          (map (partial render-team players)))]]]))
