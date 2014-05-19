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
  (let [sort-chan (om/get-state owner [:sort :chan])
        attrs     (when sort-fn {:on-click (fn [_] (put! sort-chan column))
                                 :style    {:cursor "pointer"}})]
    (if (goog/isString heading)
      [:th attrs heading]
      (->> heading (concat [:th attrs]) vec))))

(defn render-header-row [owner columns]
  [:thead [:tr (map (partial render-header-cell owner) columns)]])

(defn table [data owner {:keys [columns caption initial-sort] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:sort {:fn     (or initial-sort identity)
              :column nil
              :dir    nil
              :chan   (chan)}})

    om/IWillMount
    (will-mount [_]
      (let [sort-chan (om/get-state owner [:sort :chan])]
        (go-loop []
          (let [next-sort-column    (<! sort-chan)
                current-sort        (om/get-state owner :sort)
                same?               (= next-sort-column (:column current-sort))
                default-dir         :desc
                current-dir         (or (:dir current-sort) default-dir)
                next-dir            (if same?
                                      (if (= default-dir current-dir)
                                        :asc
                                        default-dir)
                                      default-dir)
                next-fn             (comp (partial (if (= next-dir :asc) identity reverse))
                                          (partial sort-by (comp (:sort-fn next-sort-column) (:key next-sort-column))))]
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
