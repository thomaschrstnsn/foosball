(ns foosball.table
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn render-column-in-row [row {:keys [key printer] :or {printer str}}]
  [:td (printer (key row))])

(defn render-row [columns row]
  [:tr (map (partial render-column-in-row row) columns)])

(defn render-header-row [columns]
  [:thead [:tr (map (fn [{:keys [heading]}]
                      (if (goog/isString heading)
                        [:th heading]
                        heading))
                    columns)]])

(defn table [data owner {:keys [columns caption] :as opts}]
  (reify
    om/IRenderState
    (render-state [_ state]
      (html [:table.table.table-hover
             [:caption caption]
             (render-header-row columns)
             [:tbody
              (map (partial render-row columns) data)]]))))
