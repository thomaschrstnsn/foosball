(ns foosball.editable
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [foosball.console :refer-macros [debug error]]))

(defn display [show]
  (if show
    {}
    {:display "none"}))

(defn handle-change [e data edit-key value-fn owner]
  (om/transact! data edit-key (fn [_] (-> e .-target .-value value-fn))))

(defn end-edit [data edit-key text owner]
  (om/set-state! owner :editing false)
  (om/transact! data edit-key (fn [_] text) :update))

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
  [data owner {:keys [edit-key         ;; key in data to create editable for
                      placeholder      ;; placeholder text in input field
                      input-classes    ;; additional classes for input (seq of kw or str)
                      input-props      ;; additional properties for the input element
                      change-chan      ;; channel where changes are put to, special values :editable/focus
                                        ; and :editable/blur are put for these events
                      ] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text       (get data edit-key)
            value-fn   (or value-fn identity)
            defaults   {:type "text"
                        :ref  "input"}
            must-haves {:class       (mapv name input-classes)
                        :value       text
                        :placeholder placeholder
                        :on-change   (fn [e] (handle-change e data edit-key value-fn owner))
                        :on-key-down (fn [e] (when (om/get-state owner :editing)
                                              (let [comp-kb-ev (component-keyboard-event e data)]
                                                (when (= (:key comp-kb-ev) :enter)
                                                  (.blur (om/get-node owner "input"))))))
                        :on-blur     (fn [e]
                                       (debug "blur")
                                       (when (om/get-state owner :editing)
                                         (end-edit data edit-key text owner)))
                        :on-focus    (fn [e] (om/set-state! owner :editing true))}]
        (html [:input.form-control
               (merge defaults
                      input-props
                      must-haves)])))))
