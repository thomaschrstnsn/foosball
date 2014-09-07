(ns foosball.macros)

(defmacro identity-map [& args]
  (zipmap (map keyword args) args))
