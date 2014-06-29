(ns foosball.data
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [chan <!]]
            [cljs-http.client :as http]))

(defn go-update-data! [url app key]
  (go (let [response (<! (http/get url))]
        (om/update! app key (:body response)))))
