(ns foosball.data
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [om.core :as om :include-macros true]
            [cljs-uuid-utils :as uuid]
            [cljs.core.async :refer [chan <!]]
            [cljs-http.client :as http]
            [foosball.console :refer-macros [debug debug-js info log trace error]]))

(defn get-data [url]
  (http/get url))

(defn client-data [{:keys [app key]}]
  (assert (and app key))
  (let [key (if (seq? key) key [key])]
    (get-in @app key)))

(defn go-get-data!
  "Asynchronously gets data from a server (using HTTP GET) and stores this inside given app.
  Takes a map of options which can be used to customize behaviour:
  :server-url (required) string with uri of server resource
  :app (required) the clientside app (om'y map of data)
  :key (required) kw or [kw] inside app where to check/store data
  :server-data-transform (default: identity) fn to apply to data after getting from *server*
  :satisfied-with-existing-app-data? (default: false)
  :set-to-nil-until-complete (default:false) updates value to nil while waiting for response
  :on-data-complete (default: (constantly false)) fn called for side effects when data have been gotten
  :error-handler (default: rethrows) on errors this is called"
  [{:keys [server-url app key
           satisfied-with-existing-app-data? server-data-transform on-data-complete
           set-to-nil-until-complete]
    :or {satisfied-with-existing-app-data? false
         server-data-transform identity
         on-data-complete (constantly false)
         error-handler false
         set-to-nil-until-complete false}
    :as options}]
  (assert (and server-url app key))
  (when set-to-nil-until-complete
    (om/update! app key nil))
  (let [current-value (when satisfied-with-existing-app-data? (client-data options))]
    (if-not (nil? current-value)
      (on-data-complete current-value)
      (go (let [response  (<! (get-data server-url))
                new-value (-> response :body server-data-transform)]
            (om/update! app key new-value)
            (on-data-complete new-value))))))

(defn add-uuid-key [ds]
  (mapv (fn [d] (merge d {:key (uuid/make-random-uuid)})) ds))

(defn post! [url data]
  (http/post url {:edn-params data}))

(defn throw-err [e]
  (when (instance? js/Error e) (throw e))
  e)
