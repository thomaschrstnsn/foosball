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

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

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
  [data owner {:keys [edit-key key-chan placeholder input-classes input-props
                      fn-broadcast-sub on-unmount-fn autofocus] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})

    om/IWillMount
    (will-mount [_]
      (if fn-broadcast-sub
        (go-loop []
          (try
            (let [{:keys [fn]} (<! fn-broadcast-sub)]
              (fn data owner))
            (catch js/Error e
              (error "wrong things happened: " e)))
          (recur))))

    om/IWillUnmount
    (will-unmount [_]
      (if on-unmount-fn (on-unmount-fn)))

    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text       (get data edit-key)
            defaults   {:type "text"
                        :ref  "input"}
            must-haves {:class       (mapv name input-classes)
                        :value       text
                        :placeholder placeholder
                        :on-change   #(handle-change % data edit-key owner)
                        :on-key-down #(when (om/get-state owner :editing)
                                        (let [comp-kb-ev (component-keyboard-event % data)]
                                          (when (= (:key comp-kb-ev) :enter)
                                            (.blur (om/get-node owner "input")))
                                          (when (and key-chan comp-kb-ev)
                                            (put! key-chan comp-kb-ev))))
                        :on-blur     (fn [e]
                                       (when (om/get-state owner :editing)
                                         (end-edit data edit-key text owner)))
                        :on-focus    #(om/set-state! owner :editing true)}]
        (html [:input.form-control
               (merge defaults
                      input-props
                      must-haves
                      (when autofocus {:auto-focus "autofocus"}))])))))
