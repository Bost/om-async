(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            ;; [om-async.logger-pprint :as l]
            [om-async.logger :as l]
            [om-async.utils :as u]
            [clojure.pprint :as pp] ; for debug purposes
            )
  (:use [korma.db]
        [korma.core]
        ))

(def src "db.clj")

(defn db-connect [dbase]
  {:db (name dbase)
   :user "root"
   :password ""})

(defentity information_schema-tables
  (table :information_schema.tables)
  ;; (entity-fields :table_schema :table_name :table_type :engine)
  )

(l/defnd calc-row-limit [requested-row-limit]
  (l/infod src fn-name "requested-row-limit" requested-row-limit)
  (let [highest-row-limit 3
        row-limit (min highest-row-limit requested-row-limit)]
    (if (> requested-row-limit highest-row-limit)
      (l/warn src fn-name (str "Number of selected rows limited to " highest-row-limit)))
    ;; (l/infod src fn-name "row-limit" row-limit)
    row-limit))

(l/defnd entities-with-column
  [{:keys [entities column] :as params}]
  (l/infod src fn-name "entities" entities)
  (let [ewc (for [e entities]
              (if (u/contains-value? (:fields e) column)
                (:name e)
                ;; e
                nil))
        r (into [] (remove nil? ewc))]
    (l/infod src fn-name "r " r)
    r))

(l/defnd sql-select-rows-from
  ;; "Just put a key-val pair in the client to the
  ;; (oc/edn-xhr { .. :data {:select-rows-from [ .. ]}}) and ... job done!
  ;; It pops up here just like that!"
  [{:keys [dbase table rows-displayed] :as params}]
  ;; (l/infod src fn-name "params" params)
  (defdb db (mysql (db-connect dbase)))
  (let [r (select table
                  (limit (calc-row-limit rows-displayed)))]
    r))

(declare salaries titles departments dept_emp dept_manager employees salaries titles)
(def all-entities #{salaries titles departments dept_emp dept_manager employees})

;; TODO data-with-column-value: dbase parameter should be taken into account
(l/defnd data-with-column-value
  [{:keys [dbase table rows-displayed column value] :as edn-params}]
  (let [entities-wc (entities-with-column {:entities all-entities :column column})]
    (l/infod src fn-name "entities-wc" entities-wc)
    (let [r (select table
                    (where {column value})
                    (limit (calc-row-limit rows-displayed)))
          ;; TODO put the {:dbase ... :table ... } into result r
          ;; r (remove nil? r)
          ]
      (l/infod src fn-name "r" r)
      r)))

(l/defnd sql-show-tables-from
  [{:keys [dbase rows-displayed] :as params}]
  ;; (l/infod src fn-name "dbase" dbase)
  (select information_schema-tables
          (fields ;;:table_schema
           :table_name)
          (where {:table_schema (name dbase)
                  :table_type "BASE TABLE"
                  :engine "InnoDB"})
          (limit (calc-row-limit rows-displayed))))

(def show-tables-from (memoize sql-show-tables-from))
;; (sql-show-tables-from u/e)

(defentity departments
  (pk :dept_no)
  (entity-fields :dept_no :dept_name))

(defentity dept_emp
  (has-many employees)   ; (has-many employees {:fk :emp_no})
  (has-many departments) ; (has-many departments {:fk :dept_no})
  (entity-fields :emp_no :dept_no :from_date :to_date))

(defentity dept_manager
  (has-many employees)   ; (has-many employees {:fk :emp_no})
  (has-many departments) ; (has-many departments {:fk :dept_no})
  (entity-fields :emp_no :dept_no :from_date :to_date))

(defentity employees
  (pk :emp_no)
  (entity-fields :emp_no :birth_date :first_name :last_name :gender :hire_date))

(defentity salaries
  (pk :from_date)
  (has-many employees)   ; (has-many employees {:fk :emp_no})
  (entity-fields :emp_no :from_date :salary :to_date))

(defentity titles
  (pk :from_date)
  (has-many employees)   ; (has-many employees {:fk :emp_no})
  (entity-fields :emp_no :from_date :title :to_date))

(def sources [employees departments])
(def intermediate [dept_emp dept_manager])
(def sinks [titles salaries])

;; return a vector of tables where to select
;; beware of cycles!
(defn x [entity]
  (u/contains-value? sinks entity) ;; => [entity]
  (u/contains-value? sinks entity) ;; => [entity]
  )

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

(select dept_emp
        (where {:dept_no "d006"})
        (limit 2))

(s {:dbaseN0 "employees", :tableN0 "departments",
    :colN0 "dept_name", :rowV0 "Development"} 0)

;; clean the REPL - works only in clojure not in clojurescript
;; (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))
