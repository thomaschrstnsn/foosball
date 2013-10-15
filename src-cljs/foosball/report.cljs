(ns foosball.report
  (:require [foosball.dom-utils :as u]
            [foosball.validation.match :as match-validation]
            [dommy.core :as dommy])
  (:use [jayq.core :only [$ document-ready add-class remove-class parents-until parent]])
  (:use-macros [dommy.macros :only [sel1]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn auto-submit-league-select []
  (let [input (sel1 [:#league-id])
        form  (sel1 [:#league-form])]
    (u/log "auto-submit-league-select" input form)
    (dommy/listen! input :change #(.submit form))))

(defn validation-map-with-ids [validation]
  (->> validation
       (map (fn [[k v]] {(->> k name (str "#") keyword) v}))
       (apply merge)))

(defn validation-map-nil-is-valid [validation]
  (->> validation
       (map (fn [[k v]] {k ((fnil identity true) v)}))
       (apply merge)))

(defn toggle-error-on-form-group-by-id [id error?]
  (let [form-group (-> ($ id) (parents-until :.form-group) parent)]
    (if error?
      (add-class    form-group :has-error)
      (remove-class form-group :has-error))))

(defn live-validation-of-match-report []
  (let [elem-to-chan-and-paths (apply merge
                                      (for [team [:team1 :team2]
                                            per-team [:player1 :player2 :score]
                                            :let [path    [team per-team]
                                                  id      (keyword (str "#" (name team) (name per-team)))
                                                  [el ch] (u/elem-and-chan id :change)]]
                                        {el {:chan ch :path path :id id}}))
        chans                  (->> elem-to-chan-and-paths
                                    (map (comp :chan second))
                                    vec)
        get-state              (fn [] (->> elem-to-chan-and-paths
                                          (map (fn [[e {:keys [path]}]]
                                                 [path (u/parse-int (.-value e))]))
                                          (reduce (fn [m [p v]]
                                                    (update-in m p (constantly v))) {})))
        update-ui-from-state  (fn [] (let [no-validation-ids #{:#matchdate}
                                          validation-map (->> (get-state)
                                                              match-validation/validate-report
                                                              validation-map-nil-is-valid
                                                              (filter (fn [[k v]] (not (no-validation-ids k)))))]
                                      (doseq [[id valid?] validation-map]
                                        (toggle-error-on-form-group-by-id id (not valid?)))))]
    (u/log "live-validation")
    (auto-submit-league-select)
    (update-ui-from-state)
    (go (loop []
          (let [[event _] (alts! chans)]
            (update-ui-from-state)
            (recur))))))
