(defproject om-async "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2227"]
                 [ring/ring "1.3.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.6.4"]
                 [compojure "1.1.8"]
                 [fogus/ring-edn "0.2.0"]
                 ;; [com.datomic/datomic-free "0.9.4699"]

                 [org.clojure/java.jdbc "0.3.3"]
                 [mysql/mysql-connector-java "5.1.25"]
                 ]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/clj" "src/cljs"]
              :compiler {
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js/out"
                :optimizations :none
                :source-map true}}]})
