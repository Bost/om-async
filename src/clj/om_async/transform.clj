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
      (l/infod src fn-name "result" result)
      result)))

(defn to-korks [f values]
  (let [r (map-indexed (fn [idx value] {(f idx) value}) values)
        rv (into [] r)]
    (apply merge rv)))

(defn convert-to-korks [vals-vec]
  (let [fn-name "convert-to-korks"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [rows (map (fn [row] (to-korks u/kw-row row)) vals-vec)
          rows-seq (into [] rows)
          cols (to-korks u/kw-col rows-seq)]
      ;; (l/infod src fn-name "rows" rows)
      ;; (l/infod src fn-name "rows-seq" rows-seq)
      (l/infod src fn-name "cols" cols)
      cols)))

;; (def vals-vec
;;   [["1987-06-26" "1986-06-26" "60117" "10001"]
;;    ["1988-06-25" "1987-06-26" "62102" "10001"]])
;; (convert-to-korks vals-vec)

(defn to-vals [korks]
  (into [] (vals korks)))

(defn convert-to-vals [korks]
  (into []
        (map #(into [] (vals %)) (to-vals korks))))

;; (def cols
;;   {:col1 {:row3 "10001", :row2 "62102", :row1 "1987-06-26", :row0 "1988-06-25"}
;;    :col0 {:row3 "10001", :row2 "60117", :row1 "1986-06-26", :row0 "1987-06-26"}})
;; (convert-to-vals cols)

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
          out-data (encode-entity idx :table table vals)]
      ;; (l/infod src fn-name "table" table)
      (l/infod src fn-name "data" data)
      (l/infod src fn-name "out-data" out-data)
      ;; (l/infod src fn-name "idx" idx)
      out-data)))

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
    ;; (l/infod src fn-name "obj" obj)
    (process-sql db/sql-select-rows-from obj)))

;; TODO paredit grow right should jump over comment

(defn process-show-tables-from [dbase]
  (process-sql db/sql-show-tables-from dbase 0))

(defn process-show-tables-with-data-from [dbase]
  (let [list-tables (map first (table-vals (db/show-tables-from dbase)))
        tables (into [] list-tables)
        count-tables (count tables)]
    ;; (l/infod src "process-show-tables-with-data-from" "tables: " tables)
    (map #(process-select-rows-from dbase %1 %2)
         tables
         (into [] (range count-tables)))))

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
      (l/infod src fn-name "kw-fetch-fn" kw-fetch-fn)
      (l/infod src fn-name "fetch-fn" fetch-fn)
      (l/infod src fn-name "manipulator-fn" manipulator-fn)
      (l/infod src fn-name "params" params)
      (let [r
            ;; (map #(fetch-fn %) params)
            ;; this works only for the request: select-rows-from
            (map #(fetch-fn %) params)
            ]
        ;; (l/infod src fn-name "r" r)
        (let [data (manipulator-fn r)]
          ;; (l/infod src fn-name "data" data)
          data)))))

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
