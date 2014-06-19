(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            [om-async.logger :as l]
            [om-async.utils :as u]
            )
  (:use [korma.db]
        [korma.core]
        ))

(def src "db.clj; ")

(def db-connect nil)
(def db-connect {:classname "com.mysql.jdbc.Driver"
                 :subprotocol "mysql"
                 ;; :subname "//127.0.0.1:3306/employees"
                 :subname "//127.0.0.1:3306/"
                 :user "root"
                 :password "bost"})

(defdb db (mysql db-connect))

(defentity ist
   (table :information_schema.tables)
  ;; (entity-fields :table_schema :table_name :table_type :engine)
  )

(defn sql-select-rows-from [table]
;;   (let [sql-cmd (str "select * from " table " limit 2")]
;;     ;; (l/info (str src "select-rows-from: " sql-cmd))
;;     (let [r (sql/query db-connect [sql-cmd])]
;;       ;; (l/info (str "db.clj; r: " r))
;;       (into [] r)))
  (select table
          (limit 2))
  )

;;(sql-select-rows-from u/s)

(defn sql-show-tables-from [db-name]
;;   ;; (l/info (str src "show-tables-from: Fetching data from dbase: " db-name))
;;   (let [sql-cmd (str "SHOW TABLES FROM " db-name)]
;;     ;; (l/info (str src "show-tables-from: " sql-cmd))
;;     (let [r (sql/query db-connect [sql-cmd])]
;;       ;; (l/info (str src "r: " r))
;;       (into [] r)))
  (select ist
          (fields ;;:table_schema
           :table_name)
          (where {:table_schema db-name
                  :table_type "BASE TABLE"
                  :engine "InnoDB"}))
  )

;; (def show-tables-from nil)
(def show-tables-from (memoize sql-show-tables-from))

;;(sql-show-tables-from u/e)

