(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            ;; [taoensso.timbre :as logger]
            ))

(def src "db.clj; ")

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

(def e "employees")
(def d "departments")
(def t ["employees" "departments"])

(defn sql-select-rows-from [table]
  (let [sql-cmd (str "select * from " table " limit 2")]
    (println (str src "select-rows-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str "db.clj; r: " r))
      r)))

(defn sql-show-tables-from [db-name]
  ;; (println (str src "show-tables-from: Fetching data from dbase: " db-name))
  (let [sql-cmd (str "SHOW TABLES FROM " db-name)]
    (println (str src "show-tables-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str src "r: " r))
      r)))

(def show-tables-from (memoize sql-show-tables-from))
