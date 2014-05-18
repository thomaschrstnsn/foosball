(ns foosball.console)

(def ^:dynamic *enable-console* true)

(defn stringify [args]
  (into-array [(apply pr-str-with-opts args {:readably false})]))
