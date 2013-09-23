(defproject foosball "1.2.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.4159" :exclusions [com.amazonaws/aws-java-sdk]]
                 [lib-noir "0.6.8"]
                 [compojure "1.1.5" :exclusions [org.clojure/tools.macro
                                                 org.clojure/core.incubator
                                                 ring/ring-core]]
                 [ring-server "0.3.0" :exclusions [org.clojure/core.incubator]]
                 [com.cemerick/friend "0.1.5" :exclusions [ring/ring-core
                                                           slingshot]]
                 [com.taoensso/timbre "2.6.1"]
                 [log4j "1.2.17" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [hiccup "1.0.4"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [clj-time "0.6.0"]
                 [org.clojure/tools.nrepl "0.2.3"]]

  :hooks [configleaf.hooks]
  :configleaf {:verbose true}

  :lein-release {:deploy-via :shell
                 :build-via  :lein-ring-uberwar
                 :shell ["scp" :build-artifact "webber:/var/lib/tomcat7/webapps/ROOT.war"]}

  :cljsbuild {:builds {:dev {:source-paths ["src-cljs"],
                             :compiler {:pretty-print false
                                        :output-to "resources/public/js/foosball.js"
                                        :optimizations :simple}}}}

  :ring {:handler foosball.handler/war-handler,
         :init    foosball.servlet-lifecycle/init,
         :destroy foosball.servlet-lifecycle/destroy
         :open-browser? false
         :auto-reload? false}

  :repl-options {:port 1234}

  :profiles {:production {:ring {:stacktraces? false}}

             :dev {:ring {:stacktraces? true}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/clojurescript "0.0-1859"]
                                  [org.clojure/core.async "0.1.0-SNAPSHOT"]
                                  [org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.1"]
                                  [ring-mock "0.1.5"]
                                  [ring/ring-devel "1.2.0"]
                                  [midje "1.5.1"]
                                  [server-socket "1.0.0"]
                                  [prismatic/dommy "0.1.1"]
                                  [jayq "2.4.0"]]
                   :repositories {"sonatype-oss-public"
                                  "https://oss.sonatype.org/content/groups/public/"}}}

  :url "https://foosball.chrstnsn.dk/"

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "0.3.3"]
            [configleaf "0.4.6"]
            [lein-release "1.0.4"]]

  :description "Foosball result tracking and statistics site."
  :min-lein-version "2.0.0")
