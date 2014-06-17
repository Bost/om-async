(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            ;; [taoensso.timbre :as logger]
            ))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

(def e "employees")
(def d "departments")
(def t ["employees" "departments"])

(defn select-rows-from [table]
  (let [sql-cmd (str "select * from " table " limit 2")]
    (println (str "db.clj; select-rows-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str "db.clj; r: " r))
      r)))

(defn select-rows-from-processor [table]
  ;; (println (str "select-rows-from-processor: (result (select-rows-from \"" table "\"))"))
  (result (select-rows-from table)))

(select-rows-from-processor d)

(defn show-tables-from [db-name]
  ;; (println (str "show-tables-from: Fetching data from dbase: " db-name))
  (let [sql-cmd (str "SHOW TABLES FROM " db-name)]
    (println (str "db.clj; show-tables-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str "db.clj; r: " r))
      r)))

(def show-tables-from-memo (memoize show-tables-from))

(defn show-tables-from-processor [db-name]
  ;; (println (str "show-tables-from-processor: (" (name 'show-tables-from-memo) " \"" db-name"\")"))
  (result
   (show-tables-from-memo db-name)))

(defn show-tables-with-data-from-processor [db-name]
  ;; (println (str "show-tables-with-data-from-processor: (" (name 'show-tables-from-memo) " " db-name")"))
  (let [list-tables (map first (table-vals (show-tables-from-memo db-name)))
        tables (into [] list-tables)]
    ;; (println (str "db.clj; (map " (name 'select-rows-from-processor) " " tables ")"))
    (map select-rows-from-processor tables)))
