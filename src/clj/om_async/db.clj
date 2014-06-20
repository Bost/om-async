(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            [om-async.logger :as l]
            [om-async.utils :as u]
            )
  (:use [korma.db]
        [korma.core]
        ))

(def src "db.clj")

(defn db-connect [dbase]
;;   {:classname "com.mysql.jdbc.Driver"
;;    :subprotocol "mysql"
;;    :subname (str "//127.0.0.1:3306/" dbase)
;;    :user "root"
;;    :password ""}
  {:db (name dbase)
   :user "root"
   :password ""})

(defentity ist
   (table :information_schema.tables)
  ;; (entity-fields :table_schema :table_name :table_type :engine)
  )

(defn sql-select-rows-from [dbase table]
;;   (let [sql-cmd (str "select * from " table " limit 2")]
;;     ;; (l/info (str src "select-rows-from: " sql-cmd))
;;     (let [r (sql/query db-connect [sql-cmd])]
;;       ;; (l/info (str "db.clj; r: " r))
;;       (into [] r)))
  (defdb db (mysql (db-connect dbase)))
  (select table
          (limit 2)))

(sql-select-rows-from u/e u/s)

(defn sql-show-tables-from [dbase]
;;   (let [sql-cmd (str "SHOW TABLES FROM " dbase)]
;;     ;; (l/info (str src "show-tables-from: " sql-cmd))
;;     (let [r (sql/query db-connect [sql-cmd])]
;;       ;; (l/info (str src "r: " r))
;;       (into [] r)))
  (select ist
          (fields ;;:table_schema
           :table_name)
          (where {:table_schema (name dbase)
                  :table_type "BASE TABLE"
                  :engine "InnoDB"})))

;; (def show-tables-from nil)
(def show-tables-from (memoize sql-show-tables-from))

(sql-show-tables-from u/e)

(defn s [params idx]
  (let [
        dbase ((u/kw :dbase :name idx) params)
        table ((u/kw :table :name idx) params)
        col ((u/kw :col :name idx) params)
        row-val ((u/kw :row :name idx) params)
        ]
  (defdb db (mysql (db-connect dbase)))
  (select table
          (where {col [like (str "%" row-val "%")]})
          (limit 2))))
