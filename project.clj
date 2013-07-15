(defproject foosball "0.9.11-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.3941" :exclusions [com.amazonaws/aws-java-sdk]]
                 [lib-noir "0.6.4"]
                 [compojure "1.1.5" :exclusions [org.clojure/tools.macro
                                                 ring/ring-core]]
                 [ring-server "0.2.8"]
                 [com.taoensso/timbre "2.1.2"]
                 [markdown-clj "0.9.28"]
                 [prismatic/dommy "0.1.1"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [hiccup "1.0.3"]
                 [org.clojure/math.combinatorics "0.0.4"]]

  :hooks [configleaf.hooks]
  :configleaf {:verbose true}

  :lein-release {:deploy-via :shell
                 :build-via  :lein-ring-uberwar
                 :shell ["scp" :build-artifact "webber:/var/lib/tomcat7/webapps/ROOT.war"]}

  :cljsbuild {:builds {
                       ;; :prod
                       ;; {:source-paths ["src-cljs"]
                       ;;  :compiler {:pretty-print false
                       ;;             :output-to "resources/public/js/foosball.js"
                       ;;             :optimizations :simple}}

                       :dev
                       {:source-paths ["src-cljs"],
                        :compiler {:pretty-print true
                                   :output-to "resources/public/js/foosball.js"
                                   :optimizations :whitespace}}
                       }}

  :ring {:handler foosball.handler/war-handler,
         :init foosball.servlet-lifecycle/init,
         :destroy foosball.servlet-lifecycle/destroy
         :open-browser? false
         :auto-reload? false}

  :repl-options {:port 1234}

  :profiles {:production {:ring {:stacktraces? false}}

             :dev {:ring {:stacktraces? true}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4-SNAPSHOT"]
                                  [org.clojure/java.classpath "0.2.0"]
                                  [ring-mock "0.1.5"]
                                  [ring/ring-devel "1.1.8"]
                                  [midje "1.5.1"]]
                   :repositories {"sonatype-oss-public"
                                  "https://oss.sonatype.org/content/groups/public/"}}}

  :url "https://foosball.chrstnsn.dk/"

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "0.3.0"]
            [configleaf "0.4.6"]
            [lein-release "1.0.4"]]

  :description "Foosball result tracking and statistics site."
  :min-lein-version "2.0.0")
