(defproject om-async "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [ring/ring "1.3.1"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [om "0.7.2"]
                 [compojure "1.1.9"]
                 [fogus/ring-edn "0.2.0"]
                 ;; [com.datomic/datomic-free "0.9.4699"]

                 [org.clojure/java.jdbc "0.3.5"]
                 [mysql/mysql-connector-java "5.1.32"]

                 [korma "0.4.0"] ;; sql for clojure

                 ;; logging
                 ;; [com.taoensso/timbre "3.2.1"]

                 ;; [org.clojure/tools.logging "0.3.0"]
                 ;; [org.slf4j/slf4j-log4j12 "1.7.7"]

                 [clj-time "0.8.0"]
                 ]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.keminglabs/cljx "0.4.0"
             :exclusions [org.clojure/clojure]]
            ]

  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   ;; :output-path "target/classes"
                   :output-path "src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   ;; :output-path "target/classes"
                   :output-path "src/cljs"
                   :rules :cljs}]}

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/clj" "src/cljs"]
              :compiler {
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :source-map true}}]})
