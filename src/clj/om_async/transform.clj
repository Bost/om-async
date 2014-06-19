(ns om-async.transform
  (:require [om-async.utils :as u]
            [om-async.db :as db]
            [om-async.logger :as l]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj; ")

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

(defn table-vals [data]
  (into []
        (map (fn [v]
               ;; TODO convert only dates to strings
               (into [] (map str
                             (into [] (vals v)))))
             data)))

(defn nth-from [all-vals i]
  (into [] (map #(nth % i) all-vals)))

(defn format-columns [idx column column-vals]
  {(u/kw :col :val idx)
   {:col-name column :col-vals column-vals}})

(defn encode-table [tdata tname idx]
  {(u/kw :table :name idx) tname
   (u/kw :table :val idx)
   (apply merge
          (let [all-vals (table-vals tdata)
                all-cols (table-cols tdata)
                indexes (range (count all-cols))
                table-vals (map #(nth-from all-vals %) indexes)]
            (map #(format-columns %1 %2 %3)
                 indexes
                 all-cols
                 table-vals)))})

;; every process-* function must call a function from the db-namespace
(defn process-sql [sql-fn obj idx]
  ;; (l/info (str src "process-sql: (result (sql-fn \"" table "\"))"))
  (encode-table (sql-fn obj) obj idx))

(defn process-select-rows-from [table table-idx]
  ;; (l/info (str src "process-select-rows-from: (process-sql db/sql-select-rows-from \"" table "\" " table-idx "))"))
  (process-sql db/sql-select-rows-from table table-idx))

(defn process-show-tables-from [db-name]
  ;; (l/info (str src "process-show-tables-from: (process-sql db/sql-show-tables-from \"" db-name"\")"))
  (process-sql db/sql-show-tables-from db-name 0))

(defn process-show-tables-with-data-from [db-name]
  ;; (l/info (str src "process-show-tables-with-data-from: (" (name 'show-tables-from) " " db-name")"))
  (let [list-tables (map first (table-vals (db/show-tables-from db-name)))
        tables (into [] list-tables)
        count-tables (count tables)]
    ;; (l/info (str src "(map " (name 'process-select-rows-from) " " tables ")"))
    ;; (l/info (str src "count-tables: " count-tables))
    (map #(process-select-rows-from %1 %2)
         tables
         (into [] (range count-tables)))))

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
    ;; (l/info (str src "fetch: (manipulator-fn " fetch-fn " " params ")"))
    ;; (l/info (str src "fetch: kw-fetch-fn: " kw-fetch-fn))
        (let [data (manipulator-fn (map #(fetch-fn %) params))]
          ;; (l/info (str src "fetch: data: " data))
          data)))
