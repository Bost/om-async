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
        nv (into [] n)]
    nv))

(defn table-vals [data]
  (let [fn-name "table-vals"]
    (l/infod src fn-name "data" data)
    (let [result
          (map (fn [v]
                 ;; TODO convert only dates to strings
                 (into [] (map str
                               (into [] (vals v)))))
               data)]
      ;; (l/infod src fn-name "result" result)
      result)))

(defn nth-from [all-vals idx]
  (map #(nth % idx) all-vals))

(defn encode-entity [idx prefix name vals]
  {(u/kw-prefix prefix idx)
   ;; TODO don't transfer a vector containing a single name
   {:name [name] :vals (into [] vals)}})

(defn encode-table [table data idx]
  (let [fn-name "encode-table"]
    (let [vals (let [all-vals (table-vals data)
                     all-cols (table-cols data)
                     indexes (range (count all-cols))
                     table-vals (map #(nth-from all-vals %) indexes)]
                 (map #(encode-entity %1 :col %2 %3)
                      indexes
                      all-cols
                      table-vals))
          data (encode-entity idx :table table vals)]
      ;; (l/info src fn-name (str "table: " table))
      ;; (l/info src fn-name (str "data: " data))
      ;; (l/info src fn-name (str "idx: " idx))
      data)))

;; every process-* function must call a function from om-async.db
(defn process-sql [sql-fn dbase obj idx]
  (encode-table obj (sql-fn dbase obj) idx))

(defn process-select-rows-from [dbase table table-idx]
  (process-sql db/sql-select-rows-from dbase table table-idx))

(defn process-show-tables-from [dbase]
  (process-sql db/sql-show-tables-from dbase 0))

(defn process-show-tables-with-data-from [dbase]
  (let [list-tables (map first (table-vals (db/show-tables-from dbase)))
        tables (into [] list-tables)
        count-tables (count tables)]
    ;; (l/info src "process-show-tables-with-data-from" (str "tables: " tables))
    (map #(process-select-rows-from dbase %1 %2)
         tables
         (into [] (range count-tables)))))

(defn process-request [params idx]
  (let [fn-name "process-request"]
    (let [table ((u/kw :table :name idx) params)
          data (encode-table table (db/s params idx) idx)]
      (l/info src fn-name (str "table: " table))
      ;; (l/info src fn-name (str "data: " data))
      data)))

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
        (l/info src fn-name (str "data: " data))
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
