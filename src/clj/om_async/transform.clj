(ns om-async.transform
  (:require [om-async.utils :as utils]
            [om-async.db :as db]
            [om-async.logger :as logger]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj; ")

(defn create-item [v i]
  {(utils/column-keyword i) v})

(defn table-cols [raw-data]
  (let [vx (into [] raw-data)
        ;; since the table structure is the same operate only on the first row
        ;; TODO modify relevant select to return just one row.
        row (nth vx 0)
        k (keys row)
        kv (into [] k)
        n (map #(name %) kv)
        nv (into [] n)
        vvn (into [] (map (fn [v] [v]) nv))]
    vvn))

(defn table-vals [raw-data]
  (let [data (into [] raw-data)
        out-data
        (into [] (map (fn [v]
                        ;; TODO convert only dates to strings
                        (into [] (map str
                                      (into [] (vals v)))))
                      data))]
    out-data))

(defn nth-from [all-vals i]
  (into [] (map #(nth % i) all-vals)))

(defn format-columns [index column column-vals]
  {(utils/column-keyword index)
   {:col-name column :col-vals column-vals}})

(defn encode-table [tdata tname idx]
  (let [tname-kw (utils/table-name-keyword idx)
        tdata-kw (utils/table-data-keyword idx)]
    {tname-kw tname
     tdata-kw
     (apply merge
            (let [all-vals (table-vals tdata)
                  all-cols (table-cols tdata)
                  indexes (range 0 (count all-cols))
                  table-vals (map #(nth-from all-vals %) indexes)]
              (map #(format-columns %1 %2 %3)
                   indexes
                   all-cols
                   table-vals)))}))

(def e "employees")
(def d "departments")
(def t ["employees" "departments"])

;; every process-* function must call a function from the db-namespace
(defn process-sql [sql-fn obj idx]
  ;; (logger/info (str src "process-sql: (result (sql-fn \"" table "\"))"))
  (encode-table (sql-fn obj) obj idx))

(defn process-select-rows-from [table table-idx]
  ;; (logger/info (str src "process-select-rows-from: (process-sql db/sql-select-rows-from \"" table "\" " table-idx "))"))
  (process-sql db/sql-select-rows-from table table-idx))

(defn process-show-tables-from [db-name idx]
  ;; (logger/info (str src "process-show-tables-from: (process-sql db/sql-show-tables-from \"" db-name"\")"))
  (process-sql db/sql-show-tables-from db-name ))

(defn process-show-tables-with-data-from [db-name]
  ;; (logger/info (str src "process-show-tables-with-data-from: (" (name 'show-tables-from) " " db-name")"))
  (let [list-tables (map first (table-vals (db/show-tables-from db-name)))
        tables (into [] list-tables)
        count-tables (count tables)]
    ;; (logger/info (str src "(map " (name 'process-select-rows-from) " " tables ")"))
;;     (logger/info (str src "count-tables: " count-tables))
    (map #(process-select-rows-from %1 %2)
         tables
         (into [] (range count-tables)))))

;; ({:table-name0 "departments", :table-data0 {:col0 {:col-name ["dept_name"], :col-vals ["Customer Service" "Development"]}, :col1 {:col-name ["dept_no"], :col-vals ["d009" "d005"]}}}
;;  {:table-name1 "dept_emp", :table-data1 {:col0 {:col-name ["to_date"], :col-vals ["9999-01-01" "9999-01-01"]}, :col1 {:col-name ["from_date"], :col-vals ["1986-06-26" "1996-08-03"]}, :col2 {:col-name ["dept_no"], :col-vals ["d005" "d007"]}, :col3 {:col-name ["emp_no"], :col-vals ["10001" "10002"]}}}
;;  {:table-name2 "dept_manager", :table-data2 {:col0 {:col-name ["to_date"], :col-vals ["1991-10-01" "9999-01-01"]}, :col1 {:col-name ["from_date"], :col-vals ["1985-01-01" "1991-10-01"]}, :col2 {:col-name ["emp_no"], :col-vals ["110022" "110039"]}, :col3 {:col-name ["dept_no"], :col-vals ["d001" "d001"]}}}
;;  {:table-name3 "employees", :table-data3 {:col0 {:col-name ["hire_date"], :col-vals ["1986-06-26" "1985-11-21"]}, :col1 {:col-name ["gender"], :col-vals ["M" "F"]}, :col2 {:col-name ["last_name"], :col-vals ["Facello" "Simmel"]}, :col3 {:col-name ["first_name"], :col-vals ["Georgi" "Bezalel"]}, :col4 {:col-name ["birth_date"], :col-vals ["1953-09-02" "1964-06-02"]}, :col5 {:col-name ["emp_no"], :col-vals ["10001" "10002"]}}}
;;  {:table-name4 "salaries", :table-data4 {:col0 {:col-name ["to_date"], :col-vals ["1987-06-26" "1988-06-25"]}, :col1 {:col-name ["from_date"], :col-vals ["1986-06-26" "1987-06-26"]}, :col2 {:col-name ["salary"], :col-vals ["60117" "62102"]}, :col3 {:col-name ["emp_no"], :col-vals ["10001" "10001"]}}}
;;  {:table-name5 "titles", :table-data5 {:col0 {:col-name ["to_date"], :col-vals ["9999-01-01" "9999-01-01"]}, :col1 {:col-name ["from_date"], :col-vals ["1986-06-26" "1996-08-03"]}, :col2 {:col-name ["title"], :col-vals ["Senior Engineer" "Staff"]}, :col3 {:col-name ["emp_no"], :col-vals ["10001" "10002"]}}}
;; )

(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                })

(def manipulator-fns {:select-rows-from            (fn [p] (into [] p)) ;; working with multiple tables
                                                   ;; identity          ;; working with a single table
                      :show-tables-from            (fn [p] (into [] p)) ;; working with multiple dbases
                                                   ;; identity          ;; working with a single dbase
                      :show-tables-with-data-from  ;;first
                                                   (fn [p] (into [] (first p)))
                      })

(defn fetch [edn-params]
  (let [kw-fetch-fn (nth (keys edn-params) 0)
        fetch-fn (kw-fetch-fn fetch-fns)
        manipulator-fn (kw-fetch-fn manipulator-fns)
        params (kw-fetch-fn edn-params)]
    ;; (logger/info (str src "fetch: (manipulator-fn " fetch-fn " " params ")"))
    ;; (logger/info (str src "fetch: kw-fetch-fn: " kw-fetch-fn))
        (let [data (manipulator-fn (map #(fetch-fn %) params))]
          ;; (logger/info (str src "fetch: data: " data))
          data)))
