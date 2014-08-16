(ns foosball.datepicker
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [chan <! put! unique]]
            [foosball.console :refer-macros [debug debug-js info log trace error]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn date-component [data owner {:keys [value-key placeholder change-ch]}]
  (reify
    om/IInitState
    (init-state [_]
      (let [range-changed-ch (chan 10)]
        {:range-changed-ch range-changed-ch
         :range-changed-fn (fn [pos e] (put! range-changed-ch [pos (.. e -target -value)]))}))

    om/IWillMount
    (will-mount [_]
      (let [range-changed-ch (unique (om/get-state owner :range-changed-ch))]
        ;; uses buffered and unique to avoid multiple triggers on the same logical event,
        ;; bug in bootstrap-datepicker?
        (go-loop []
          (let [[pos val] (<! range-changed-ch)
                key       (get {:to to-key
                                :from from-key} pos)]
            (debug "got change:" pos val key)
;            (om/transact! data key (fn [_] val))
            )
          (recur))))

    om/IDidMount
    (did-mount [_]
      (let [range-changed-fn (om/get-state owner :range-changed-fn)
            $daterange  (js/$ (om/get-node owner "date-range"))
            $datepicker (.datepicker $daterange #js {:language "da"
                                                     :autoclose true
                                                     :calendarWeeks true
                                                     :format "dd/mm/yyyy"
                                                     :todayHighlight true
                                                     :weekStart 1})
            $from-date (js/$ (om/get-node owner "from-date"))
            $to-date (js/$ (om/get-node owner "to-date"))]
        (.on $from-date "changeDate" (partial range-changed-fn :from))
        (.on $to-date   "changeDate" (partial range-changed-fn :to))))

    om/IRender
    (render [_]
      (let [from-value (get data from-key)
            to-value   (get data to-key)
            _          (debug "datetime-range render from" from-value "to" to-value)]
        (html
         [:form.form-horizontal
          [:div.input-daterange.form-group.input-group {:ref "date-range"}
           [:input.input-sm.form-control {:type "text"
                                          :name "start"
                                          :ref "from-date"
                                          :placeholder from-placeholder
                                          :value from-value}]
           [:span.input-group-addon "til og med"]
           [:input.input-sm.form-control {:type "text"
                                          :name "end"
                                          :ref "to-date"
                                          :placeholder to-placeholder
                                          :value to-value}]]])))))

(defn daterange-component [data owner {:keys [to-key from-key from-placeholder to-placeholder]}]
  (reify
    om/IInitState
    (init-state [_]
      (let [range-changed-ch (chan 10)]
        {:range-changed-ch range-changed-ch
         :range-changed-fn (fn [pos e] (put! range-changed-ch [pos (.. e -target -value)]))}))

    om/IWillMount
    (will-mount [_]
      (let [range-changed-ch (unique (om/get-state owner :range-changed-ch))]
        ;; uses buffered and unique to avoid multiple triggers on the same logical event,
        ;; bug in bootstrap-datepicker?
        (go-loop []
          (let [[pos val] (<! range-changed-ch)
                key       (get {:to to-key
                                :from from-key} pos)]
            (debug "got change:" pos val key)
            (om/transact! data key (fn [_] val)))
          (recur))))

    om/IDidMount
    (did-mount [_]
      (let [range-changed-fn (om/get-state owner :range-changed-fn)
            $daterange  (js/$ (om/get-node owner "date-range"))
            $datepicker (.datepicker $daterange #js {:language "da"
                                                     :autoclose true
                                                     :calendarWeeks true
                                                     :format "dd/mm/yyyy"
                                                     :todayHighlight true
                                                     :weekStart 1})
            $from-date (js/$ (om/get-node owner "from-date"))
            $to-date (js/$ (om/get-node owner "to-date"))]
        (.on $from-date "changeDate" (partial range-changed-fn :from))
        (.on $to-date   "changeDate" (partial range-changed-fn :to))))

    om/IRender
    (render [_]
      (let [from-value (get data from-key)
            to-value   (get data to-key)
            _          (debug "datetime-range render from" from-value "to" to-value)]
        (html
         [:form.form-horizontal
          [:div.input-daterange.form-group.input-group {:ref "date-range"}
           [:input.input-sm.form-control {:type "text"
                                          :name "start"
                                          :ref "from-date"
                                          :placeholder from-placeholder
                                          :value from-value}]
           [:span.input-group-addon "til og med"]
           [:input.input-sm.form-control {:type "text"
                                          :name "end"
                                          :ref "to-date"
                                          :placeholder to-placeholder
                                          :value to-value}]]])))))
