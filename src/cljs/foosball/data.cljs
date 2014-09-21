(ns foosball.data
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [cljs-uuid-utils :as uuid]
            [cljs.core.async :refer [chan <!]]
            [cljs-http.client :as http]))

(defn go-update-data!
  ([url app key fn]
      (go (let [response (<! (http/get url))]
            (om/update! app key (-> response :body fn)))))
  ([url app key] (go-update-data! url app key identity)))

(defn add-uuid-key [ds]
  (mapv (fn [d] (merge d {:key (uuid/make-random-uuid)})) ds))

(defn post! [url data]
  (http/post url {:edn-params data}))

(defn throw-err [e]
  (when (instance? js/Error e) (throw e))
  e)
