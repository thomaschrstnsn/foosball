(ns foosball.browser
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:use [jayq.core :only [$ document-ready add-class remove-class parents-until parent]])
  (:require [dommy.core :as dommy]
            [foosball.dom-utils :as u]
            [clojure.browser.repl :as repl]
            [foosball.report :as report]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn navbar-elements []
  (sel [".nav" :li]))

(defn current-navbar-element []
  (->> (navbar-elements)
       (filter (fn [e] (-> (dommy/html e)
                          (.indexOf (u/current-path))
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

(defn enable-submit-on-enough-players []
  (let [[select-el select-chan] (u/elem-and-chan :#playerids :change)
        button                  (sel1 :button)]
    (go (loop []
          (let [event           (<! select-chan)
                target          (.-target event)
                selected        ($ "option:selected" target)
                enough-players? (<= 4 (count selected))]
            (if enough-players?
              (u/enable-el button)
              (u/disable-el button))
            (recur))))))

(def page-fns {"/player/log"   auto-submit-playerlog
               "/matchup"      enable-submit-on-enough-players
               "/report/match" report/live-validation-of-match-report})

(defn ^:export page-loaded []
  ;(repl/connect "http://localhost:9000/repl")
  (let [auto-fn (get page-fns (u/current-path) nil)]
    (set-active-navbar-element!)
    (when auto-fn (auto-fn))))

(defn ^:export register-document-ready []
  (document-ready page-loaded))

(defn ^:export page-autorefresh [seconds]
  (js/setTimeout #(.reload js/location)
                 (* seconds 1000)))
