(ns om-async.transform
  (:require [om-async.utils :as u]
            [om-async.db :as db]
            [om-async.logger :as l]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj")

(defn table-vals [data]
  (let [fn-name "table-vals"]
    (l/infod src fn-name "data" data)
    (let [result
          (map (fn [v]
                 ;; TODO convert only dates to strings
                 (into [] (map str
                               (into [] (vals v)))))
               data)]
      (l/infod src fn-name "result" result)
      result)))

(defn nth-from [all-vals idx]
  (map #(nth % idx) all-vals))

(defn encode-entity [idx prefix name vals]
  {(u/kw-prefix prefix idx)
   ;; TODO don't transfer a vector containing a single name
   {:name [name] :vals (into [] vals)}})

(def built-in-formatter (tf/formatters :mysql))

(defn convert-val [v]
  (let [fn-name "convert-val"]
    ;; (l/infod src fn-name "v" v)
    (if (instance? java.util.Date v)
      (tf/unparse built-in-formatter (tc/from-date v))
      v)))

(defn convert-hashmap [m]
  "Apply convert-val on each value of hash-map m"
  (reduce-kv (fn [m k v]
               (assoc m k (convert-val v))) {} m))

(defn encode-table [table data idx]
  (let [fn-name "encode-table"]
    ;; (l/infod src fn-name "table" table)
    ;; (l/infod src fn-name "data" data)
    (let [encoded-table (map #(convert-hashmap %) data)]
      ;; (l/infod src fn-name "encoded-table" encoded-table)
      ;; (l/infod src fn-name "idx" idx)
      encoded-table)))

;; every process-* function must call a function from om-async.db
(defn process-sql [sql-fn {:keys [dbase table idx]}]
  "idx is index for the keyword :table in the returned hash-map"
  (let [fn-name "process-sql"]
    ;; (l/infod src fn-name "dbase" dbase)
    ;; (l/infod src fn-name "table" table)
    ;; (l/infod src fn-name "idx" idx)
    (encode-table table (sql-fn {:dbase dbase :table table}) idx)))

(defn process-select-rows-from [obj]
  (let [fn-name "process-select-rows-from"]
    (l/infod src fn-name "obj" obj)
    (let [r (process-sql db/sql-select-rows-from obj)]
      (l/infod src fn-name "r" r)
      r)))

;; TODO paredit grow right should jump over comment

(defn process-show-tables-from [dbase]
  (process-sql db/sql-show-tables-from dbase 0))

(defn process-show-tables-with-data-from [dbase]
  (let [fn-name "process-show-tables-with-data-from"]
    (l/infod src fn-name "dbase" dbase)
    (let [list-tables (map first (table-vals (db/show-tables-from dbase)))
          tables (into [] list-tables)
          count-tables (count tables)]
      ;; (l/infod src "process-show-tables-with-data-from" "tables: " tables)
      (map #(process-select-rows-from {:dbase dbase :table %1 :idx %2})
           tables
           (into [] (range count-tables))))))

(defn process-request [params idx]
  (let [fn-name "process-request"]
    (let [table ((u/kw :table :name idx) params)
          data (encode-table table (db/s params idx) idx)]
      (l/infod src fn-name "table" table)
      ;; (l/infod src fn-name "data" data)
      data)))

(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                :request                    process-request
                })

(defn m-x [params data]
  (let [fn-name "m-x"]
    (l/infod src fn-name "params" params)
    (l/infod src fn-name "data" data)
    (let [vals-vec (u/convert-to-korks u/kw-row data)
          rx (first vals-vec)]
      (l/infod src fn-name "vals-vec" vals-vec)
      (l/infod src fn-name "rx" rx)
      rx)))

(defn m-select-rows-from [params data]
  (let [fn-name "m-select-rows-from"]
    (l/infod src fn-name "params" params)
    (l/infod src fn-name "data" data)
    (let [rlist
            (doall (map (fn [p d]
                          (merge p
                                 {:data
                                 (m-x p [d])}
                                 ))
                        params data))
          r (into [] rlist)
          ]
      (l/infod src fn-name "r" r)
      r)))

(defn m-show-tables-from [p]
  (into [] p))

(defn m-show-tables-with-data-from [p]
  (into [] (first p)))

(defn m-request [p]
  (into [] p))

(def manipulator-fns {:select-rows-from            m-select-rows-from
                      :show-tables-from            m-show-tables-from
                      :show-tables-with-data-from  m-show-tables-with-data-from
                      :request                     m-request
                      })

(defn fetch [edn-params]
  (let [fn-name "fetch"]
    (let [;; TODO get the content of N-th key? this could be done better
          kw-fetch-fn (nth (keys edn-params) 0)
          fetch-fn (kw-fetch-fn fetch-fns)
          manipulator-fn (kw-fetch-fn manipulator-fns)
          params (kw-fetch-fn edn-params)]
      (l/infod src fn-name "kw-fetch-fn" kw-fetch-fn)
      (l/infod src fn-name "fetch-fn" fetch-fn)
      (l/infod src fn-name "manipulator-fn" manipulator-fn)
      (l/infod src fn-name "params" params)
      (let [raw-data (into [] (map #(fetch-fn %) params))
            r (manipulator-fn params raw-data)
            ]
        (l/infod src fn-name "raw-data" raw-data)
        (l/infod src fn-name "r" r)
        r))))

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
