(ns om-async.transform
  (:require [om-async.utils :as u]
            [om-async.db :as db]
;;             [om-async.logger-pprint :as l]
            [om-async.logger :as l]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj")

(l/defnd table-vals [data]
  ;; (l/infod src fn-name "data" data)
  (let [result
        (map (fn [v]
               ;; TODO convert only dates to strings
               (into [] (map str
                             (into [] (vals v)))))
             data)]
    ;; (l/infod src fn-name "result" result)
    result))

(defn nth-from [all-vals idx]
  (map #(nth % idx) all-vals))

(defn encode-entity [idx prefix name vals]
  {(u/kw-prefix prefix idx)
   ;; TODO don't transfer a vector containing a single name
   {:name [name] :vals (into [] vals)}})

(def built-in-formatter (tf/formatters :mysql))

(l/defnd convert-val [v]
  ;; (l/infod src fn-name "v" v)
  (if (instance? java.util.Date v)
    (tf/unparse built-in-formatter (tc/from-date v))
    v))

(defn convert-hashmap [m]
  "Apply convert-val on each value of hash-map m"
  (reduce-kv (fn [m k v]
               (assoc m k (convert-val v))) {} m))

(l/defnd encode-table
  [{:keys [table data idx] :as params}]
  ;; "idx is index for the keyword :table in the returned hash-map"

  ;; (l/infod src fn-name "table" table)
  ;; (l/infod src fn-name "data" data)
  (let [encoded-table (map convert-hashmap data)]
    ;; (l/infod src fn-name "encoded-table" encoded-table)
    ;; (l/infod src fn-name "idx" idx)
    encoded-table))

;; every process-* function must call a function from om-async.db
(l/defnd process-sql [sql-fn {:keys [dbase table idx] :as params}]
  ;; "idx is index for the keyword :table in the returned hash-map"
  ;; (l/infod src fn-name "sql-fn" sql-fn)
  ;; (l/infod src fn-name "dbase" dbase)
  ;; (l/infod src fn-name "table" table)
  ;; (l/infod src fn-name "idx " idx)
  (encode-table {:table table :data (sql-fn params) :idx idx})
  ;; table (sql-fn params) idx)
  )

(l/defnd process-select-rows-from [obj]
  (l/infod src fn-name "obj" obj)
  (let [r (into [] (process-sql db/sql-select-rows-from obj))]
    (l/infod src fn-name "r" r)
    r))

;; TODO paredit grow right should jump over comment
(defn process-show-tables-from [obj]
  (process-sql db/sql-show-tables-from obj))

;; TODO DRY! see onclick/kw-table
(defn kw-table [idx]
  (keyword (str "table" idx)))

(l/defnd process-show-tables-with-data-from
  [{:keys [dbase rows-displayed] :as params}]
  ;; (l/infod src fn-name "params" params)
  (let [
        all-tables (table-vals (db/show-tables-from params))
        list-tables (map first all-tables)
        tables (into [] list-tables)
        count-tables (count tables)
        ]
;;     (l/infod src fn-name "all-tables" all-tables)
;;     (l/infod src fn-name "tables" tables)
;;     (l/infod src fn-name "count-tables" count-tables)
    ;; TODO process tables
    (let [r
          []
          ;; TODO implement process-show-tables-with-data-from
          ;; (map #(process-select-rows-from
          ;;        {:dbase dbase :table %1 :rows-displayed 2 :idx (kw-table %2)})
          ;;      tables
          ;;      (into [] (range count-tables)))
          ]
      (l/infod src fn-name "r" r)
      r)))

(l/defnd process-request [params idx]
  (let [table ((u/kw :table :name idx) params)
        data (encode-table {:table table :data (db/s params idx) :idx idx})]
    ;; (l/infod src fn-name "table" table)
    ;; (l/infod src fn-name "data" data)
    data))

(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                :request                    process-request
                })

(l/defnd m-x [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (let [vals-vec (u/convert-to-korks u/kw-row data)
        rx (first vals-vec)]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    ;; (l/infod src fn-name "rx" rx)
    rx))

(l/defnd m-x-one [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (let [vals-vec (u/convert-to-korks u/kw-row data)
        rx (first vals-vec)]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    ;; (l/infod src fn-name "rx" rx)
    rx))

(l/defnd m-select-rows-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (def params [{:dbase "employees", :table "departments", :idx 0}])
  ;; (l/infod src fn-name "data" data)
  ;; (def data [({:dept_no "d009", :dept_name "Customer Service"}
  ;;             {:dept_no "d005", :dept_name "Development"})])
  (let [rlist
        (doall (map (fn [p d]
                      (merge p
                             {:data (m-x p [d])}))
                    params data))
        rvec (into [] rlist)
        ks (into [] (map #(keyword (str "table" %)) (range (count rvec))))
        rdata (zipmap ks rvec)
        ;; TODO extend-table must be done in the client.cljs
        ]
    ;; (l/infod src fn-name "rvec" rvec)
    ;;(println "rdata" rdata)
    rdata
    ))

(l/defnd m-show-tables-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (def params [{:dbase "employees" :idx 0}]})
  ;; (l/infod src fn-name "data" data)
  ;; (def data [({:table_name "departments"} {:table_name "dept_emp"} {:table_name "dept_manager"}
  ;;             {:table_name "employees"} {:table_name "salaries"} {:table_name "titles"})])
  (m-select-rows-from params data))

(l/defnd m-show-tables-with-data-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (m-select-rows-from params data))

(defn m-request [p]
  (into [] p))

(def manipulator-fns {:select-rows-from            m-select-rows-from
                      :show-tables-from            m-show-tables-from
                      :show-tables-with-data-from  m-show-tables-with-data-from
                      :request                     m-request
                      })


(l/defnd get-params-for-fetch [src]
  (into []
        (for [kw-dbase (keys (get-in src [:data]))
              kw-table (keys (get-in src [:data kw-dbase :data]))]
          (let [
                ;;idx-kw-dbase idx-kw-table
                ;;kw-dbase           (nth (keys (get-in src [:data])) idx-kw-dbase)
                ;;idx-dbase          (subs (name kw-dbase) (count "dbase") (count (name kw-dbase)))
                ;;kw-table           (nth (keys (get-in src [:data kw-dbase :data])) idx-kw-table)
                ;;idx-table          (subs (name kw-table) (count "table") (count (name kw-table)))

                get-table-name     [:data kw-dbase :data kw-table :name]
                table-name         (get-in src get-table-name)

                get-dbase-name     [:data kw-dbase :name]
                dbase-name         (get-in src get-dbase-name)

                get-rows-displayed [:data kw-dbase :data kw-table :data :rows-displayed]
                rows-displayed     (get-in src get-rows-displayed)
                r {:dbase dbase-name :table table-name :rows-displayed rows-displayed :idx kw-table}
                ]
            ;; (l/infod src fn-name "kw-dbase" kw-dbase)
            ;; (l/infod src fn-name "idx-dbase" idx-dbase)
            ;; (l/infod src fn-name "kw-table" kw-table)
            ;; (l/infod src fn-name "idx-table" idx-table)
            ;; (l/infod src fn-name "r" r)
            r)
          )))

(l/defnd fetch [edn-params]
  ;; (l/infod src fn-name "new-edn-params" new-edn-params)
  (let [;; TODO get the content of N-th key? this could be done better
        kw-fetch-fn (:name edn-params)
        fetch-fn (kw-fetch-fn fetch-fns)
        manipulator-fn (kw-fetch-fn manipulator-fns)
        params (get-params-for-fetch edn-params)
        ]
    (l/infod src fn-name "edn-params" edn-params)
    (l/infod src fn-name "kw-fetch-fn" kw-fetch-fn)
    (l/infod src fn-name "fetch-fn" fetch-fn)
    (l/infod src fn-name "manipulator-fn" manipulator-fn)
    (l/infod src fn-name "params" params)
    (let [data (into [] (map fetch-fn params))]
      (l/infod src fn-name "data" data)
      (let [r (manipulator-fn params data)]
        (l/infod src fn-name "r" r)
        r))))

(l/defnd request [edn-params]
  (l/infod src fn-name "edn-params" edn-params)
;;   (let [
;;         kw-fetch-fn (nth (keys edn-params) 0)
;;         fetch-fn (kw-fetch-fn fetch-fns)
;;         manipulator-fn (kw-fetch-fn manipulator-fns)
;;         params (kw-fetch-fn edn-params)
;;         val-params (into [] (vals params))
;;         ]
;;     ;; (l/info src fn-name (str "kw-fetch-fn: " kw-fetch-fn))
;;     ;; (l/info src fn-name (str "fetch-fn: " fetch-fn))
;;     ;; (l/info src fn-name (str "manipulator-fn: " manipulator-fn))
;;     ;; (l/info src fn-name (str "params: " params))
;;     ;; (l/info src fn-name (str "val-params: " val-params))
;;     (let [f0 (fetch-fn params 0)] ;; onClick sends just 1 value
;;       ;; (l/info src fn-name (str "f0: " f0))
;;       (let [data (manipulator-fn f0)]
;;         ;; (l/infod src fn-name "data" data)
;;         data)))
  "TODO send edn-params to db.clj; db.clj should find entities with given :column"
  )

;; clean the REPL - works only in clojure not in clojurescript
;; (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))
