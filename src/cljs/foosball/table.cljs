(ns foosball.table
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [foosball.macros :refer [identity-map]])
  (:require [foosball.console :refer-macros [debug debug-js info log trace error warn]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponentk]]
            [cljs.core.async :refer [chan <! put!]]
            [sablono.core :as html :refer-macros [html]]
            [schema.core :as s]))

(defcomponentk datacell-component
  [[:data row column default-container default-align :as data]
   owner]
  (render [_]
    (html
     (let [{:keys [key printer fn align]
            :or {printer str}} column
            align              (or align default-align)
            value              ((comp printer fn) row)]
       [:td
        (condp = align
          :right {:class "text-right"}
          :center {:class "text-center"}
          nil)
        (if default-container
          [default-container value]
          value)]))))

(defcomponentk row-component
  [[:data columns row key row-class-fn default-container default-align :as data]
   owner]
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
               (om/build datacell-component col
                         {:fn        (fn [column]
                                       (merge {:key col-key-str}
                                              (identity-map row column default-align default-container)))
                          :react-key col-key-str})))
           columns)])))

(defcomponentk header-cell-component
  [[:data default-container
    [:column heading {sort-fn nil} :as column]]
   owner]
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

(defcomponentk header-row-component [[:data columns default-container] owner]
  (render-state [_ {:keys [sort-chan]}]
    (html [:tr
           (om/build-all header-cell-component
                         columns
                         {:state {:sort-chan sort-chan}
                          :fn    (fn [column]
                                   (identity-map column default-container))})])))

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
   :rows    [s/Any]
   (s/optional-key :default-align) Alignment
   (s/optional-key :class) s/Any                 ;; class property for the table
   (s/optional-key :default-container) s/Keyword ;; hiccup keyword to put values and header into
   (s/optional-key :row-class-fn) Fny            ;; applied to each row, result is used as :class prop
   (s/optional-key :caption) Hiccup})

(defcomponentk table
  [[:data columns rows
    {caption nil}
    {default-align :left}
    {default-container nil}
    {class nil}
    {row-class-fn (constantly nil)}] :- TableOpts
   owner]
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
      (html [:table.table
             (when class {:class class})
             (when caption [:caption caption])
             [:thead (om/build header-row-component columns
                               {:react-key "headerrow"
                                :fn        (fn [columns] (identity-map columns default-container row-class-fn))
                                :state     {:sort-chan sort-chan}})]
             [:tbody
              (map (fn [{:keys [key] :as row}]
                     (let [key (when key (str key))]
                       (om/build row-component row
                                 (merge {:fn (fn [row] (identity-map row columns key row-class-fn
                                                                    default-container default-align))}
                                        (when key {:react-key key})))))
                   (sort-fn rows))]]))))
