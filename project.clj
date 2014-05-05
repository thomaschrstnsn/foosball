(defproject foosball "1.3.1-SNAPSHOT"
  :jvm-opts ["-Xmx1g" "-server"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.9.4724" :exclusions [com.amazonaws/aws-java-sdk]]
                 [ring/ring-core "1.2.0" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/data.json "0.2.4"]
                 [liberator "0.11.0" :exlusions [org.clojure/data.json]]
                 [lib-noir "0.8.2"]
                 [compojure "1.1.6" :exclusions [org.clojure/tools.macro
                                                 org.clojure/core.incubator
                                                 ring/ring-core]]
                 [ring-server "0.3.0" :exclusions [org.clojure/core.incubator]]
                 [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache]]
                 [com.taoensso/timbre "3.1.6"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [hiccup "1.0.5"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [clj-time "0.6.0"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.3"]]

  :hooks [configleaf.hooks]
  :configleaf {:verbose false}

  :lein-release {:deploy-via :shell
                 :build-via  :lein-ring-uberwar
                 :shell ["echo" "built: " :build-artifact]}

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src-cljs"],
                :compiler {:pretty-print false
                           :output-to "resources/public/js/dev/foosball.js"
                           :output-dir "resources/public/js/dev"
                           :optimizations :none
                           :source-map true}}
               {:id "production"
                :source-paths ["src-cljs"],
                :compiler {:pretty-print false
                           :output-to "resources/public/js/foosball.js"
                           :optimizations :advanced
                           :externs ["externs/jquery-1.9.js"]}}]}

  :ring {:handler foosball.servlet-lifecycle/handler,
         :init    foosball.servlet-lifecycle/init,
         :destroy foosball.servlet-lifecycle/destroy
         :open-browser? false
         :auto-reload? false}

  :repl-options {:port 1234
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles {:production {:ring {:stacktraces? false}
                          :dependencies [[org.clojure/tools.reader "0.7.10"]]}

             :dev {:ring {:stacktraces? true}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/clojurescript "0.0-2030"]
                                  [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                                  [org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [ring-mock "0.1.5"]
                                  [ring/ring-devel "1.2.0"]
                                  [midje "1.6.3" :exclusions [org.codehaus.plexus/plexus-utils
                                                              slingshot
                                                              commons-codec]]
                                  [server-socket "1.0.0"]
                                  [prismatic/dommy "0.1.1"]
                                  [secretary "0.4.0" :exclusions [org.clojure/clojurescript]]
                                  [jayq "2.5.0"]]
                   :plugins      [[com.cemerick/austin "0.1.3"]]
                   :repositories {"sonatype-oss-public"
                                  "https://oss.sonatype.org/content/groups/public/"}}}

  :url "https://foosball.chrstnsn.dk/"

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "1.0.0-alpha2"]
            [configleaf "0.4.6"]
            [lein-release "1.0.4"]
            [lein-midje "3.1.3"]]

  :aliases {"build-jar" ["with-profile" "production" "ring" "uberjar"]
            "build-war" ["with-profile" "production" "ring" "uberwar"]
            "ci" ["with-profile" "dev" "do"
                  "cljsbuild" "once,"
                  "midje,"
                  "build-jar,"
                  "build-war"]}

  :description "Foosball result tracking and statistics site."
  :min-lein-version "2.0.0")
