(defproject foosball "0.8.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.3941"]
                 [lib-noir "0.5.2"]
                 [compojure "1.1.5"]
                 [ring-server "0.2.8"]
                 [com.taoensso/timbre "1.6.0"]
                 [markdown-clj "0.9.19"]
                 [prismatic/dommy "0.1.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [hiccup "1.0.3"]
                 [midje "1.5.1"]]

  :hooks [configleaf.hooks]
  :configleaf {:verbose true}

  :lein-release {:deploy-via :shell
                 :shell ["echo" "releasing..."]}

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
         :init foosball.handler/init,
         :destroy foosball.handler/destroy}

  :profiles {:production {:ring
                          {:open-browser? false, :stacktraces? false, :auto-reload? false}},
             :dev {:dependencies [[ring-mock "0.1.3"] [ring/ring-devel "1.1.8"]]}}

  :url "https://foosball.chrstnsn.dk/"

  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "0.3.0"]
            [configleaf "0.4.6"]
            [lein-release "1.0.4"]]

  :description "Foosball result tracking and statistics site."
  :min-lein-version "2.0.0")
