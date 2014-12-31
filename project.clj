(defproject om-async "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [ring/ring "1.3.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-beta5"]
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
                 ]

  :plugins [[lein-cljsbuild "1.0.4"]
            [com.keminglabs/cljx "0.5.0"
             ;;:exclusions [org.clojure/clojure]
             ]
            ]
  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "src/cljs"
                   :rules :cljs}]}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/clj" "src/cljs"]
              :compiler {
                ;; :output-to default: target/cljsbuild-main.js
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                ;; can not use :optimizations :whitespace
                ;; otherwise the build process complains
                :optimizations :none
                :source-map true}}]})
