(ns foosball.browser
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:use [jayq.core :only [$ document-ready add-class remove-class parents-until parent]])
  (:require [dommy.core :as dommy]
            [clojure.browser.repl :as repl]
            [foosball.validation.match :as match-validation]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn log [& items]
  (.log js/console (apply str (interpose ", " items))))

(defn current-path []
  window.location.pathname)

(defn navbar-elements []
  (sel [".nav" :li]))

(defn current-navbar-element []
  (->> (navbar-elements)
       (filter (fn [e] (-> (dommy/html e)
                          (.indexOf (current-path))
                          (>= 0))))
       first))

(defn set-active-navbar-element! []
  (let [el (current-navbar-element)]
    (when el
      (dommy/add-class! el :active))))

(defn auto-submit-playerlog []
  (let [input (sel1 [:#playerid])
        form  (sel1 [:form])]
    (dommy/listen! input :change #(.submit form))))

(defn event-chan [el event-type]
  (let [c (chan)]
    (dommy/listen! el event-type #(put! c %))
    c))

(defn elem-and-chan [sel-arg event-type]
  (let [el (sel1 sel-arg)
        ch (event-chan el event-type)]
    [el ch]))

(defn enable-el [el]
  (dommy/remove-attr! el :disabled))

(defn disable-el [el]
  (dommy/set-attr! el :disabled "disabled"))

(defn enable-submit-on-enough-players []
  (let [[select-el select-chan] (elem-and-chan :#playerids :change)
        button                  (sel1 :button)]
    (go (loop []
          (let [event           (<! select-chan)
                target          (.-target event)
                selected        ($ "option:selected" target)
                enough-players? (<= 4 (count selected))]
            (if enough-players?
              (enable-el button)
              (disable-el button))
            (recur))))))

(defn parse-int [s]
  (let [parsed (js/parseInt s)]
    (if (js/isNaN parsed)
      nil
      parsed)))

(defn validation-map-with-ids [validation]
  (->> validation
       (map (fn [[k v]] {(->> k name (str "#") keyword) v}))
       (apply merge)))

(defn validation-map-nil-is-valid [validation]
  (->> validation
       (map (fn [[k v]] {k ((fnil identity true) v)}))
       (apply merge)))

(defn toggle-error-on-control-group-by-id [id error?]
  (let [control-group (-> ($ id) (parents-until :.control-group) parent)]
    (if error?
      (add-class    control-group :error)
      (remove-class control-group :error))))

(defn live-validation-of-match-report []
  (let [elem-to-chan-and-paths (apply merge
                                      (for [team [:team1 :team2]
                                            per-team [:player1 :player2 :score]
                                            :let [path    [team per-team]
                                                  id      (keyword (str "#" (name team) (name per-team)))
                                                  [el ch] (elem-and-chan id :change)]]
                                        {el {:chan ch :path path :id id}}))
        chans                  (->> elem-to-chan-and-paths
                                    (map (comp :chan second))
                                    vec)
        get-state              (fn [] (->> elem-to-chan-and-paths
                                          (map (fn [[e {:keys [path]}]]
                                                 [path (parse-int (.-value e))]))
                                          (reduce (fn [m [p v]]
                                                    (update-in m p (constantly v))) {})))
        update-ui-from-state  (fn [] (let [no-validation-ids #{:#matchdate}
                                          validation-map (->> (get-state)
                                                              match-validation/validate-report
                                                              validation-map-with-ids
                                                              validation-map-nil-is-valid
                                                              (filter (fn [[k v]] (not (no-validation-ids k)))))]
                                      (doseq [[id valid?] validation-map]
                                        (toggle-error-on-control-group-by-id id (not valid?)))))]
    (update-ui-from-state)
    (go (loop []
          (let [[event _] (alts! chans)]
            (update-ui-from-state)
            (recur))))))

(def page-fns {"/player/log"   auto-submit-playerlog
               "/matchup"      enable-submit-on-enough-players
               "/report/match" live-validation-of-match-report })

(defn ^:export page-loaded []
  ;(repl/connect "http://localhost:9000/repl")
  (let [auto-fn (get page-fns (current-path) nil)]
    (set-active-navbar-element!)
    (when auto-fn (auto-fn))))

(defn ^:export register-document-ready []
  (document-ready page-loaded))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
