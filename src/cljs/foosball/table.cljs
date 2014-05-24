(ns foosball.table
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [chan <! put!]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn render-column-in-row [{:keys [default-container default-align] :as opts}
                            row
                            {:keys [key printer align] :or {printer str}}]
  (let [align (or align default-align)
        value (printer (key row))]
    [:td (when (= :right align) {:class "text-right"})
     (if default-container
       [default-container value]
       value)]))

(defn render-row [opts columns row]
  [:tr (map (partial render-column-in-row opts row) columns)])

(defn render-header-cell [owner {:keys [default-container] :as opts} {:keys [heading sort-fn key] :as column}]
  (let [sort      (om/get-state owner :sort)
        sort-chan (om/get-state owner :sort-chan)
        attrs     (when sort-fn {:on-click (fn [_] (put! sort-chan column))
                                 :style    {:cursor "pointer"}})
        sort-elem (when (= column (:column sort))
                    [:span.pull-right.text-info.glyphicon {:class (if (= :asc (:dir sort))
                                                                    "glyphicon-sort-by-attributes"
                                                                    "glyphicon-sort-by-attributes-alt")} ])]
    [:th attrs
     (if default-container
       [default-container heading]
       heading)
     sort-elem]))

(defn render-header-row [owner opts columns]
  [:thead [:tr (map (partial render-header-cell owner opts) columns)]])

(defn make-sort-fn [dir sort-column]
  (if sort-column
    (comp (partial (if (= dir :asc) identity reverse))
          (partial sort-by (comp (:sort-fn sort-column) (:key sort-column))))
    identity))

(defn table [data owner {:keys [columns caption default-align default-container class] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:sort      {:column nil
                   :dir    :desc}
       :sort-chan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [sort-chan (om/get-state owner [:sort-chan])]
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
                                   default-dir)]
            (om/update-state! owner :sort (fn [current]
                                            (merge current
                                                   {:column next-sort-column
                                                    :dir    next-dir})))
            (recur)))))

    om/IRenderState
    (render-state [_ {:keys [sort] :as state}]
      (let [sort-fn (make-sort-fn (:dir sort) (:column sort))]
        (html [:table.table (when class {:class class})
               [:caption caption]
               (render-header-row owner opts columns)
               [:tbody
                (map (partial render-row opts columns) (sort-fn data))]])))))
