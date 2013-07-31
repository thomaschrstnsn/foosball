(ns foosball.util
  "Shim for the serverside util ns, needed for the shared validation")

(defn parsable-date? [d]
  (when d) true)
