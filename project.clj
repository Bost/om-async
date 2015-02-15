(defproject om-async "0.1.0-SNAPSHOT"
  ;; TODO use core.typed - optional type checking
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2760"]

                 ;; use React without Add-Ons
                 [org.omcljs/om "0.8.8"]

                 ;; use React with Add-Ons
                 ;; [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 ;; [cljsjs/react-with-addons "0.12.2-4"]

                 [ring/ring "1.3.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [compojure "1.3.1"]
                 [fogus/ring-edn "0.2.0"]
                 ;; [com.datomic/datomic-free "0.9.4699"]

                 [org.clojure/java.jdbc "0.3.6"]
                 ;; TODO take a look at [clojure.jdbc "0.3.0"]

                 [mysql/mysql-connector-java "5.1.34"]

                 [korma "0.4.0"] ;; sql for clojure

                 ;; logging
                 ;; [com.taoensso/timbre "3.2.1"]

                 ;; [org.clojure/tools.logging "0.3.0"]
                 ;; [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [prismatic/om-tools "0.3.10"]
                 ;; [com.keminglabs/cljx "0.5.0" :exclusions [org.clojure/clojure]]
                 [clj-time "0.8.0"]
                 [omdev "0.1.3-SNAPSHOT"] ; data inspection & history component
                 ;; TODO see om-draggable

                 ;; [com.cemerick/piggieback "0.1.5"]
                 ;; [figwheel "0.2.3-SNAPSHOT"] ;; build your cljs code and hot load it into the browser
                 ]
  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-ring "0.9.1"]
            ;; [lein-figwheel "0.2.3-SNAPSHOT"]
            [com.keminglabs/cljx "0.5.0"]
            [cider/cider-nrepl "0.9.0-SNAPSHOT"]
            ]
  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "src/cljs"
                   :rules :cljs}]}

  ;; :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/clj" "src/cljs"]
              :compiler {
                ;; :output-to default: target/cljsbuild-main.js
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :externs ["resources/public/js/jquery-2.1.1.js"]
                ;; can not use :optimizations :whitespace
                ;; otherwise the build process complains
                :optimizations :none
                :source-map true}}]}

  :ring {:handler om-async.server/server}

  ;; :figwheel {
  ;;            :http-server-root "public" ;; this will be in resources/
  ;;            ;; :server-port is ignored - see server.clj (defonce server ...)
  ;;            :server-port 8080                   ;; default is 3449

  ;;            ;; CSS reloading (optional)
  ;;            ;; :css-dirs has no default value
  ;;            ;; if :css-dirs is set figwheel will detect css file changes and
  ;;            ;; send them to the browser
  ;;            :css-dirs ["resources/public/css"]

  ;;            ;; Server Ring Handler (optional)
  ;;            ;; if you want to embed a ring handler into the figwheel http-kit
  ;;            ;; server
  ;;            ;; :ring-handler om-async.server/handler

  ;;            ;; To be able to open files in your editor from the heads up display
  ;;            ;; you will need to put a script on your path.
  ;;            ;; that script will have to take a file path and a line number
  ;;            ;; ie. in  ~/bin/myfile-opener
  ;;            ;; #! /bin/sh
  ;;            ;; emacsclient -n +$2 $1
  ;;            ;;
  ;;            :open-file-command "myfile-opener"  ;; TODO fiwgwheel: myfile-opener

  ;;            ;; if you want to disable the REPL
  ;;            ;; :repl false

  ;;            ;; to configure a different figwheel logfile path
  ;;            ;; :server-logfile "tmp/logs/figwheel-logfile.log"

  ;;            }
  )
