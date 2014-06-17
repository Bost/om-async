(ns om-async.transform
  (:require [om-async.utils :as utils]
            [om-async.db :as db]
            ;; [taoensso.timbre :as logger]
            ))

;; Transformation layer between Ring and DB access functions here.

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

(defn result [raw-data]
  (let [table-kw (utils/table-keyword 0)]
    {table-kw
     (apply merge
            (let [all-vals (table-vals raw-data)
                  all-cols (table-cols raw-data)
                  indexes (range 0 (count all-cols))
                  table-vals (map #(nth-from all-vals %) indexes)]
              (map #(format-columns %1 %2 %3)
                   indexes
                   all-cols
                   table-vals)))}))


;; every process-* function must call a function from the db-namespace
(defn process-sql [sql-fn obj]
  ;; (println (str "process-sql: (result (sql-fn \"" table "\"))"))
  (result (sql-fn obj)))

(defn process-select-rows-from [table]
  ;; (println (str "process-select-rows-from: (process-sql db/sql-select-rows-from \"" table "\"))")
  (process-sql db/sql-select-rows-from table))

(defn process-show-tables-from [db-name]
  ;; (println (str "process-show-tables-from: (process-sql db/sql-show-tables-from \"" db-name"\")"))
  (process-sql db/sql-show-tables-from db-name))

(defn process-show-tables-with-data-from [db-name]
  ;; (println (str "process-show-tables-with-data-from: (" (name 'show-tables-from) " " db-name")"))
  (let [list-tables (map first (table-vals (db/show-tables-from db-name)))
        tables (into [] list-tables)]
    ;; (println (str "db.clj; (map " (name 'process-select-rows-from) " " tables ")"))
    (map process-select-rows-from tables)))



(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                })

(def manipulator-fns {:select-rows-from            (fn [p] (into [] p)) ;; working with multiple tables
                                                   ;; identity          ;; working with a single table
                      :show-tables-from            (fn [p] (into [] p)) ;; working with multiple dbases
                                                   ;; identity          ;; working with a single dbase
                      :show-tables-with-data-from  first ;;(fn [p] (into [] p))
                      })

(defn fetch [edn-params]
  (let [kw-fetch-fn (nth (keys edn-params) 0)
        fetch-fn (kw-fetch-fn fetch-fns)
        manipulator-fn (kw-fetch-fn manipulator-fns)
        params (kw-fetch-fn edn-params)]
    ;; (println (str "fetch: (dispatch " fetch-fn " " params ")"))
    ;; (println (str "fetch: kw-fetch-fn: " kw-fetch-fn))
        (let [data (manipulator-fn (map #(fetch-fn %) params))]
          ;; (println "data: " data)
          data)))
