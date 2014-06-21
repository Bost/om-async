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
  {:db (name dbase)
   :user "root"
   :password ""})

(defentity ist
   (table :information_schema.tables)
  ;; (entity-fields :table_schema :table_name :table_type :engine)
  )

(defn sql-select-rows-from [dbase table]
  (defdb db (mysql (db-connect dbase)))
  (select table
          (limit 2)))

;; (sql-select-rows-from u/e u/s)

(defn sql-show-tables-from [dbase]
  (select ist
          (fields ;;:table_schema
           :table_name)
          (where {:table_schema (name dbase)
                  :table_type "BASE TABLE"
                  :engine "InnoDB"})))

(def show-tables-from (memoize sql-show-tables-from))

;; (sql-show-tables-from u/e)

(declare salaries titles departments dept_emp  dept_manager employees salaries titles)

(defentity departments)
(defentity dept_emp)
(defentity dept_manager)
(defentity employees
        (has-many salaries)
        (has-many titles)
        (has-many dept_manager)
        (has-many dept_emp))

(defn s [params idx]
  (let [fn-name "s"
        dbase ((u/kw :dbase :name idx) params)
        table ((u/kw :table :name idx) params)
        col ((u/kw :col :name idx) params)
        row-val ((u/kw :row :val idx) params)
        ;; "Customer Service"
        ;; "Development"
        ]
    (defdb db (mysql (db-connect dbase)))
;;     (l/info src fn-name (str "params: " params))
;;     (l/info src fn-name (str "table: " table))
;;     (l/info src fn-name (str "col: " col))
;;     (l/info src fn-name (str "row-val: " row-val))
;;     (l/info src fn-name (str "kw-row-val: " (u/kw :row :val idx)))
    (select table
            ;; (where {col [like (str "%" row-val "%")]})
            (where {(keyword col) row-val})
            (limit 2))))

(s {:dbaseN0 "employees", :tableN0 "departments",
    :colN0 "dept_name", :rowV0 "Development"} 0)

