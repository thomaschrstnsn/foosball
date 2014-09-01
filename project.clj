(defproject foosball "1.4.0-SNAPSHOT"
  :jvm-opts ["-Xmx1g" "-server" "-XX:MaxPermSize=128M"]

  :url "https://foosball.chrstnsn.dk/"
  :description "Foosball result tracking and statistics site."

  :source-paths ["src/clj"]
  :test-paths  ["test/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4724" :exclusions [com.amazonaws/aws-java-sdk]]
                 [ring/ring-core "1.2.0" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/data.json "0.2.5"]
                 [liberator "0.12.0"]
                 [lib-noir "0.8.2"]
                 [compojure "1.1.6" :exclusions [org.clojure/core.incubator
                                                 ring/ring-core]]
                 [ring-server "0.3.0" :exclusions [org.clojure/core.incubator]]
                 [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache]]
                 [com.taoensso/timbre "3.1.6"]
                 [hiccup "1.0.5"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [clj-time "0.6.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [prismatic/schema "0.2.2"]]

  :profiles {:production {:ring {:stacktraces? false}
                          :dependencies []
                          :aot :all}

             :dev {:ring {:stacktraces? true}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [ring-mock "0.1.5"]
                                  [ring/ring-devel "1.2.0"]
                                  [pjstadig/humane-test-output "0.6.0"]
                                  [org.clojure/test.check "0.5.8"]
                                  [org.clojars.runa/conjure "2.1.3"]
                                  [server-socket "1.0.0"]
                                  [http-kit "2.1.18"]
                                  [figwheel "0.1.4-SNAPSHOT"]
                                  ;; clojurescript deps
                                  [org.clojure/clojurescript "0.0-2202"]
                                  [org.clojure/core.async "0.1.298.0-2a82a1-alpha"]
                                  [om "0.7.1"]
                                  [sablono "0.2.22"]
                                  [secretary "1.0.2"]
                                  [cljs-http "0.1.10" :exclusions [commons-codec]]
                                  [com.andrewmcveigh/cljs-time "0.1.4"]
                                  [org.clojars.franks42/cljs-uuid-utils "0.1.3"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}}

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "1.0.3"]
            [configleaf "0.4.6"]
            [lein-release "1.0.4"]
            [com.cemerick/clojurescript.test "0.3.1"]
            [lein-figwheel "0.1.4-SNAPSHOT"]]

  :hooks [configleaf.hooks]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {:pretty-print false
                                   :output-to "resources/public/js/dev/foosball.js"
                                   :output-dir "resources/public/js/dev"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "testable" :source-paths ["src/cljs" "test/cljs"]
                        :notify-command ["run-cljs-phantom.sh"]
                        :compiler {:output-to     "target/cljs/testable.js"
                                   :source-map    "target/cljs/testable.js.map"
                                   :output-dir    "target/cljs/test"
                                   :optimizations :none
                                   :pretty-print  true}}]
              :test-commands {"unit" ["run-cljs-phantom.sh"]}}

  :configleaf {:verbose false}

  :lein-release {:deploy-via :shell
                 :build-via  :lein-ring-uberwar
                 :shell ["echo" "built: " :build-artifact]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :css-dirs ["resources/public/css"]}

  :ring {:handler foosball.servlet-lifecycle/handler,
         :init    foosball.servlet-lifecycle/init,
         :destroy foosball.servlet-lifecycle/destroy
         :open-browser? false
         :auto-reload? false}

  :repl-options {:port 1234}

  :aliases {"deps-tree-prod" ["with-profile" "production" "deps" ":tree"]
            "deps-tree-dev" ["with-profile" "dev" "deps" ":tree"]
            "build-jar" ["with-profile" "production" "ring" "uberjar"]
            "build-war" ["with-profile" "production" "ring" "uberwar"]
            "clean-all" ["do" "cljsbuild" "clean," "clean"] ;; we cannot rely on :hooks [leiningen.cljsbuild]
            "ci" ["with-profile" "dev" "do"
                  "cljsbuild" "once,"
                  "test,"
                  "build-war"]
            "auto-cljs" ["do"
                         "cljsbuild" "clean,"
                         "cljsbuild" "auto" "dev" "testable"]}

  :min-lein-version "2.0.0")
