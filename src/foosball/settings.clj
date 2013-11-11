(ns foosball.settings)

(def cljs-optimized?
  "indicates if optimizations were used on cljsbuild.
   When it there are the compiled JavaScript is loaded in a slightly different way."
  true)

(def default-datomic-uri
  "Connection string to the Datomic database."
  "datomic:free://localhost:4334/foosball")
