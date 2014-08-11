(ns foosball.editable
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [foosball.console :refer-macros [debug error]]))

(defn notify-change [chan type val]
  (put! chan [type val]))

(defn on-change [chan owner e]
  (let [val (-> e .-target .-value)]
    (om/set-state! owner :current-value val)
    (notify-change chan ::change val)))

(defn begin-edit [chan owner val]
  (om/set-state! owner :editing true)
  (notify-change chan ::focus val))

(defn end-edit [chan owner val]
  (om/set-state! owner :editing false)
  (notify-change chan ::blur val))

(defn component-keyboard-event [ev data]
  (let [keycode (.-keyCode ev)
        codes   {13 :enter
                 38 :up
                 40 :down}
        key-kw  (get codes keycode)
        modifiers (->> (map (fn [is? key] (when is? key))
                            [(.-ctrlKey ev) (.-shiftKey ev) (.-altKey ev) (.-metaKey ev)]
                            [:ctrl :shift :alt :meta])
                       (filter identity)
                       set)]
    (when key-kw {:key key-kw :modifiers modifiers :data @data})))

(defn editable
  [data owner {:keys [value-fn         ;; fn to apply editable data to get str (default: identity)
                      placeholder      ;; placeholder text in input field
                      input-classes    ;; additional classes for input (seq of kw or str)
                      input-props      ;; additional properties for the input element
                      change-ch        ;; channel where changes are put to, as [type value] tuples,
                                        ; where type in #{:editable/focus :editable/change :editable/blur}
                      ] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false
       :current-value ((or value-fn identity) data)})

    om/IRenderState
    (render-state [_ {:keys [editing current-value]}]
      (let [defaults   {:type "text"
                        :ref  "input"}
            must-haves {:class       (mapv name input-classes)
                        :value       current-value
                        :placeholder placeholder
                        :on-change   (fn [e] (on-change change-ch owner e))
                        :on-key-down (fn [e] (when (om/get-state owner :editing)
                                              (let [comp-kb-ev (component-keyboard-event e data)]
                                                (when (= (:key comp-kb-ev) :enter)
                                                  (.blur (om/get-node owner "input"))))))
                        :on-blur     (fn [e] (when (om/get-state owner :editing)
                                              (end-edit change-ch owner current-value)))
                        :on-focus    (fn [e] (begin-edit change-ch owner current-value))}]
        (html [:input.form-control
               (merge defaults
                      input-props
                      must-haves)])))))
