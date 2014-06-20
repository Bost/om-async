(ns om-async.transform
  (:require [om-async.utils :as u]
            [om-async.db :as db]
            [om-async.logger :as l]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj")

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
  (let [fn-name "encode-table"
        data
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
                       table-vals)))}
        ]
    ;; (l/info src fn-name (str "tdata: " tdata))
    ;; (l/info src fn-name (str "tname: " tname))
    ;; (l/info src fn-name (str "idx: " idx))
    ;; (l/info src fn-name (str "data: " data))
    data))

;; every process-* function must call a function from om-async.db
(defn process-sql [sql-fn dbase obj idx]
  (encode-table (sql-fn dbase obj) obj idx))

(defn process-select-rows-from [dbase table table-idx]
  (process-sql db/sql-select-rows-from dbase table table-idx))

(defn process-show-tables-from [dbase]
  (process-sql db/sql-show-tables-from dbase 0))

(defn process-show-tables-with-data-from [dbase]
  (let [list-tables (map first (table-vals (db/show-tables-from dbase)))
        tables (into [] list-tables)
        count-tables (count tables)]
    ;; (l/info src "" (str "tables: " tables))
    (map #(process-select-rows-from dbase %1 %2)
         tables
         (into [] (range count-tables)))))

(defn process-request [params idx]
  (let [table ((u/kw :table :name idx) params)
        data (encode-table (db/s params idx) table idx)]
    ;; (l/info src "process-s" (str "table: " table))
    ;; (l/info src "process-s" (str "data: " data))
    data))

(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                :request                    process-request
                })

(def manipulator-fns {:select-rows-from            (fn [p] (into [] p)) ;; working with multiple tables
                                                   ;; identity          ;; working with a single table
                      :show-tables-from            (fn [p] (into [] p)) ;; working with multiple dbases
                                                   ;; identity          ;; working with a single dbase
                      :show-tables-with-data-from  ;;first
                                                   (fn [p] (into [] (first p)))
                      :request                     (fn [p] (into [] p)) ;; working with multiple dbases
                                                   ;; identity          ;; working with a single dbase
                      })

(defn fetch [edn-params]
  (let [fn-name "fetch"]
    (let [kw-fetch-fn (nth (keys edn-params) 0)
          fetch-fn (kw-fetch-fn fetch-fns)
          manipulator-fn (kw-fetch-fn manipulator-fns)
          params (kw-fetch-fn edn-params)
          ]
      ;; (l/info src fn-name (str "kw-fetch-fn: " kw-fetch-fn))
      ;; (l/info src fn-name (str "fetch-fn: " fetch-fn))
      ;; (l/info src fn-name (str "manipulator-fn: " manipulator-fn))
      ;; (l/info src fn-name (str "params: " params))
      (let [data (manipulator-fn (map #(fetch-fn %) params))]
        ;; (l/info src fn-name (str "data: " data))
        data))))

(defn request [edn-params]
  (let [fn-name "request"]
    ;; (l/info src fn-name (str "edn-params: " edn-params))
    (let [kw-fetch-fn (nth (keys edn-params) 0)
          fetch-fn (kw-fetch-fn fetch-fns)
          manipulator-fn (kw-fetch-fn manipulator-fns)
          params (kw-fetch-fn edn-params)
          val-params (into [] (vals params))
          ]
      ;; (l/info src fn-name (str "kw-fetch-fn: " kw-fetch-fn))
      ;; (l/info src fn-name (str "fetch-fn: " fetch-fn))
      ;; (l/info src fn-name (str "manipulator-fn: " manipulator-fn))
      ;; (l/info src fn-name (str "params: " params))
      ;; (l/info src fn-name (str "val-params: " val-params))
      (let [f0 (fetch-fn params 0)] ;; onClick sends just 1 value
        ;; (l/info src fn-name (str "f0: " f0))
        (let [data (manipulator-fn f0)]
          ;; (l/info src fn-name (str "data: " data))
          data)))))
