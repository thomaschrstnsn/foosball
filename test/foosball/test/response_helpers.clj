(ns foosball.test.response-helpers
  (:require [clojure.string :as str]))

(defn is-without-cookies? [resp]
  (let [set-cookie-header (get (:headers resp) "Set-Cookie")]
    (and (empty? set-cookie-header)
         (not= nil set-cookie-header))))

(defn content-type [response]
  (-> response
      (get-in [:headers "Content-Type"] "")
      (str/split #";")))
