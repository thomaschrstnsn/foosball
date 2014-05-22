(ns foosball.table
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [chan <! put!]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn render-column-in-row [row {:keys [key printer] :or {printer str}}]
  [:td (printer (key row))])

(defn render-row [columns row]
  [:tr (map (partial render-column-in-row row) columns)])

(defn render-header-cell [owner {:keys [heading sort-fn key] :as column}]
  (let [sort      (om/get-state owner :sort)
        sort-chan (:chan sort)
        attrs     (when sort-fn {:on-click (fn [_] (put! sort-chan column))
                                 :style    {:cursor "pointer"}})
        sort-elem (when (= column (:column sort))
                    [:span.pull-right.text-info.glyphicon {:class (if (= :asc (:dir sort))
                                                                    "glyphicon-sort-by-attributes"
                                                                    "glyphicon-sort-by-attributes-alt")} ])]
    (if (goog/isString heading)
      [:th attrs heading sort-elem]
      (->> heading (concat [:th attrs]) vec))))

(defn render-header-row [owner columns]
  [:thead [:tr (map (partial render-header-cell owner) columns)]])

(defn make-sort-fn [dir sort-column]
  (comp (partial (if (= dir :asc) identity reverse))
        (partial sort-by (comp (:sort-fn sort-column) (:key sort-column)))))

(defn table [data owner {:keys [columns caption default-sort] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      (let [sort-column (->> columns
                             (filter (fn [{:keys [key]}] (= (:key default-sort) key)))
                             first)
            sort-fn     (when default-sort (make-sort-fn (:dir default-sort) sort-column))]
        {:sort {:fn     (or sort-fn identity)
                :column sort-column
                :dir    (or (:dir default-sort) :desc)
                :chan   (chan)}}))

    om/IWillMount
    (will-mount [_]
      (let [sort-chan (om/get-state owner [:sort :chan])]
        (go-loop []
          (let [next-sort-column (<! sort-chan)
                current-sort     (om/get-state owner :sort)
                same?            (= next-sort-column (:column current-sort))
                default-dir      :desc
                current-dir      (or (:dir current-sort) default-dir)
                next-dir         (if same?
                                   (if (= default-dir current-dir)
                                     :asc
                                     default-dir)
                                   default-dir)
                next-fn          (make-sort-fn next-dir next-sort-column)]
            (om/update-state! owner :sort (fn [current]
                                            (merge current
                                                   {:fn     next-fn
                                                    :column next-sort-column
                                                    :dir    next-dir})))
            (recur)))))

    om/IRenderState
    (render-state [_ state]
      (let [sort-fn (om/get-state owner [:sort :fn])]
        (html [:table.table.table-hover
               [:caption caption]
               (render-header-row owner columns)
               [:tbody
                (map (partial render-row columns) (sort-fn data))]])))))
