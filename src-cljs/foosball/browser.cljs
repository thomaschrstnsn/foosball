(ns foosball.browser
  (:use-macros [dommy.macros :only [sel sel1 nodes]])
  (:use [jayq.core :only [$ document-ready]])
  (:require [dommy.core :as dommy]
            [clojure.browser.repl :as repl]
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

(defn enable-el [el]
  (dommy/remove-attr! el :disabled))

(defn disable-el [el]
  (dommy/set-attr! el :disabled "disabled"))

(defn enable-submit-on-enough-players []
  (let [select-el   (sel1 [:#playerids])
        select-chan (event-chan select-el :change)
        button      (sel1 :button)]
    (go (loop []
          (let [event           (<! select-chan)
                target (.-target event)
                selected ($ "option:selected" target)
                enough-players? (<= 4 (count selected))]
            (if enough-players?
              (enable-el button)
              (disable-el button))
            (recur))))))

(def page-fns {"/player/log" auto-submit-playerlog
               "/matchup"    enable-submit-on-enough-players})

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
