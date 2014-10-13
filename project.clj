(defproject foosball "1.4.0-SNAPSHOT"
  :jvm-opts ["-Xmx1g" "-server" "-XX:MaxPermSize=128M"]

  :url "https://foosball.chrstnsn.dk/"
  :description "Foosball result tracking and statistics site."

  :source-paths ["src/clj"]
  :test-paths  ["test/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4724" :exclusions [com.amazonaws/aws-java-sdk]]
                 [ring/ring-core "1.3.1"]
                 [org.clojure/data.json "0.2.5"]
                 [liberator "0.12.2"]
                 [lib-noir "0.8.9"]
                 [compojure "1.2.0"]
                 [ring-server "0.3.1"]
                 [org.clojure/core.cache "0.6.4"]
                 [com.cemerick/friend "0.2.1"]
                 [com.taoensso/timbre "3.3.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [clj-time "0.8.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/tools.nrepl "0.2.6"]
                 [prismatic/schema "0.3.0"]]

  :profiles {:production {:ring {:stacktraces? false}
                          :dependencies []
                          :aot :all}

             :dev {:ring {:stacktraces? true}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.7"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.1"]
                                  [pjstadig/humane-test-output "0.6.0"]
                                  [org.clojure/test.check "0.5.9"]
                                  [org.clojars.runa/conjure "2.2.0"]
                                  [server-socket "1.0.0"]
                                  [http-kit "2.1.19"]
                                  [figwheel "0.1.4-SNAPSHOT"]
                                  ;; clojurescript deps
                                  [org.clojure/clojurescript "0.0-2202"]
                                  [org.clojure/core.async "0.1.298.0-2a82a1-alpha"]
                                  [om "0.7.1"]
                                  [prismatic/om-tools "0.3.2" :exclusions [org.clojure/clojure]]
                                  [sablono "0.2.22"]
                                  [secretary "1.0.2"]
                                  [cljs-http "0.1.15" :exclusions [commons-codec]]
                                  [com.andrewmcveigh/cljs-time "0.1.4"]
                                  [org.clojars.franks42/cljs-uuid-utils "0.1.3"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}}

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "1.0.3"]
            [configleaf "0.4.6"]
            [com.jakemccrary/lein-test-refresh "0.5.4"]
            [com.cemerick/clojurescript.test "0.3.1"]
            [lein-figwheel "0.1.4-SNAPSHOT"]]

  :hooks [configleaf.hooks]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs" "dev/cljs"]
                        :compiler {:pretty-print false
                                   :output-to "resources/public/js/dev/foosball.js"
                                   :output-dir "resources/public/js/dev"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "production"
                        :source-paths ["src/cljs"]
                        :compiler {:pretty-print false
                                   :output-to "resources/public/js/foosball.js"
                                   :optimizations :advanced
                                   :preamble ["react/react.min.js"]
                                   :externs ["extern/jquery-1.9.js"
                                             "react/externs/react.js"]}}
                       {:id "testable" :source-paths ["src/cljs" "test/cljs"]
                        :notify-command ["./run-cljs-phantom.sh"]
                        :compiler {:output-to     "target/cljs/testable.js"
                                   :source-map    "target/cljs/testable.js.map"
                                   :output-dir    "target/cljs/test"
                                   :optimizations :none
                                   :pretty-print  true}}]
              :test-commands {"unit" ["run-cljs-phantom.sh"]}}

  :configleaf {:verbose false}

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

            ;; we cannot rely on :hooks [leiningen.cljsbuild]
            "clean-all" ["do" "cljsbuild" "clean," "clean"]

            "build-jar" ["with-profile" "production" "ring" "uberjar"]
            "build-war" ["with-profile" "production" "ring" "uberwar"]

            "ci"        ["with-profile" "dev" "do"
                         "cljsbuild" "once,"
                         "test,"
                         "build-war"]

            "prod-build" ["do"
                          "clean-all,"
                          "cljsbuild" "once" "production,"
                          "build-war"]

            ;; dev
            "cljs-autotest"   ["do"
                               "cljsbuild" "clean" "testable,"
                               "cljsbuild" "auto" "testable"]
            "cljs-figwheel"   ["do"
                               "cljsbuild" "clean" "dev,"
                               "figwheel" "dev"]
            "cljs-production" ["do"
                               "cljsbuild" "clean" "production,"
                               "cljsbuild" "auto" "production"]
            "clj-autotest"    ["test-refresh"]

            ;; ring
            "ring-prod-like" ["with-profile" "production"
                              "ring" "server-headless"]}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["prod-build"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]]

  :min-lein-version "2.4.0")
