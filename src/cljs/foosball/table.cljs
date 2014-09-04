(ns foosball.table
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [foosball.console :refer-macros [debug debug-js info log trace error warn]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [schema.core :as s]))

(defcomponent datacell-component [{:keys [row column] :as data}
                                  owner
                                  {:keys [default-container default-align] :as opts}]
  (render [_]
    (html
     (let [{:keys [key printer fn align]
            :or {printer str}} column
            align              (or align default-align)
            value              ((comp printer fn) row)]
       [:td (when (= :right align) {:class "text-right"})
        (if default-container
          [default-container value]
          value)]))))

(defcomponent row-component
  [{:keys [columns row key] :as data}
   owner
   {:keys [row-class-fn] :as opts :or {row-class-fn (constantly nil)}}]
  (render
      [_]
    (html
     [:tr {:class (row-class-fn row)
           :key key}
      (map (fn [col]
             (let [col-fn-is-key? (-> col :fn keyword?)
                   col-key        (or (-> col :key)
                                      (when col-fn-is-key?
                                        (-> col :fn)))
                   _              (when-not col-key (error "no column key for column: " col))
                   __             (when-not (keyword? col-key)
                                    (error "non-kw column key for column: " col-key))
                   col-key-str    (name col-key)]
               (om/build datacell-component col {:opts      opts
                                                 :fn        (fn [c]
                                                              {:row row
                                                               :column c
                                                               :key col-key-str})
                                                 :react-key col-key-str})))
           columns)])))

(defcomponent header-cell-component
  [{:keys [heading sort-fn key] :as column}
   owner
   {:keys [default-container] :as opts}]
  (render-state [_ {:keys [sort sort-chan] :as state}]
    (let [attrs     (when sort-fn {:on-click (fn [_] (put! sort-chan column))
                                   :style    {:cursor "pointer"}})
          sort-elem (when (= column (:column sort))
                      [:span.pull-right.text-info.glyphicon
                       {:class (if (= :asc (:dir sort))
                                 "glyphicon-sort-by-attributes"
                                 "glyphicon-sort-by-attributes-alt")} ])]
      (html
       [:th attrs
        (if default-container
          [default-container heading]
          heading)
        sort-elem]))))

(defcomponent header-row-component [columns owner opts]
  (render-state [_ {:keys [sort-chan]}]
    (html [:tr (om/build-all header-cell-component columns {:opts opts
                                                            :state {:sort-chan sort-chan}})])))

(defn make-sort-fn [dir sort-column]
  (if sort-column
    (comp (partial (if (= dir :asc) identity reverse))
          (partial sort-by (comp (:sort-fn sort-column) (:fn sort-column))))
    identity))

(def Fny    (s/pred ifn?))

(def Vector (s/pred vector?))

(def Hiccup (s/either Vector s/Str))

(def Alignment (s/enum :left :right :center))

(def Column
  {:fn Fny                           ;; extracts column data (CD) out of each row
   (s/optional-key :key) s/Keyword   ;; for react-keys, used only when :fn is not kw
   (s/optional-key :printer) Fny     ;; is applied to CD, expected to return: hiccup or string (default: str)
   (s/optional-key :sort-fn) Fny     ;; indicates sortable column, value is used with CD to sort-by on rows
   (s/optional-key :align) Alignment ;; horizontal
   (s/optional-key :heading) Hiccup})

(def TableOpts
  {:columns [Column]
   (s/optional-key :default-align) Alignment
   (s/optional-key :class) s/Any                 ;; class property for the table
   (s/optional-key :default-container) s/Keyword ;; hiccup keyword to put values and header into
   (s/optional-key :row-class-fn) Fny            ;; applied to each row, result is used as :class prop
   (s/optional-key :caption) Hiccup})

(defcomponent table
  [data
   owner
   {:keys [columns caption default-align default-container class] :as opts} :- TableOpts]
  (init-state [_]
    {:sort      {:column nil
                 :dir    :desc}
     :sort-chan (chan)})

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

  (render-state [_ {:keys [sort sort-chan] :as state}]
    (let [sort-fn (make-sort-fn (:dir sort) (:column sort))]
      (html [:table.table (when class {:class class})
             [:caption caption]
             [:thead (om/build header-row-component columns {:opts opts
                                                             :react-key "headerrow"
                                                             :state {:sort-chan sort-chan}})]
             [:tbody
              (map (fn [{:keys [key] :as row}]
                     (let [key (when key (str key))]
                       (om/build row-component row (merge {:opts opts
                                                           :fn (fn [d] {:row d
                                                                       :columns columns
                                                                       :key key})}
                                                          (when key
                                                            {:react-key key})))))
                   (sort-fn data))]]))))
