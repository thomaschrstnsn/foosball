(ns foosball.test.response-helpers)

(defn is-without-cookies? [resp]
  (let [set-cookie-header (get (:headers resp) "Set-Cookie")]
    (and (empty? set-cookie-header)
         (not= nil set-cookie-header))))
