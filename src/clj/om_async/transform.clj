(ns om-async.transform
  (:require [om-async.utils :as u]
            [om-async.db :as db]
;;             [om-async.logger-pprint :as l]
            [om-async.logger :as l]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clojure.pprint :as pp] ; for debug purposes
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "transform.clj")

;; TODO convert only dates to strings
(l/defnd date-to-string [v]
  (l/info src fn-name (str "(type v): " (type v)))
  (println (str "(type v): " (type v)))
  ;;(if (instance? java.util.Date v)
    (into [] (map str
                  (into [] (vals v))))
    ;; v)
  )

(l/defnd table-vals [data]
  (l/infod src fn-name "data" data)
  (let [result (map date-to-string data)]
    ;; (l/infod src fn-name "result" result)
    result))

(l/defnd nth-from [all-vals idx]
  (map #(nth % idx) all-vals))

(l/defnd encode-entity [idx prefix name vals]
  {(u/kw-prefix prefix idx)
   ;; TODO don't transfer a vector containing a single name
   {:name [name] :vals (into [] vals)}})

(def built-in-formatter (tf/formatters :mysql))

;; Conversion between java.util.Date and clj-time
(l/defnd convert-val [v]
  ;; (l/infod src fn-name "v" v)
  (if (instance? java.util.Date v)
    (tf/unparse built-in-formatter (tc/from-date v))
    v))

(l/defnd convert-hashmap [m]
  "Apply convert-val on each value of hash-map m"
  (reduce-kv (fn [m k v]
               (assoc m k (convert-val v))) {} m))

(l/defnd encode-table
  [{:keys [table data idx] :as params}]
  ;; "idx is index for the keyword :table in the returned hash-map"
  ;; (l/infod src fn-name "table" table)
  ;; (l/infod src fn-name "data" data)
  (let [r (map convert-hashmap data)]
    ;; (l/infod src fn-name "r" r)
    ;; (l/infod src fn-name "idx" idx)
    r))

;; every process-* function must call a function from om-async.db
(l/defnd process-sql [sql-fn {:keys [dbase table idx] :as params}]
  ;; "idx is index for the keyword :table in the returned hash-map"
  (l/infod src fn-name "params" params)
  (encode-table {:table table :data (sql-fn params) :idx idx}))

(l/defnd process-data-with-column-value [obj]
  (l/infod src fn-name "obj" obj)
  (let [r (process-sql db/data-with-column-value obj)]
    ;; (l/infod src fn-name "r" r)
    r))

(l/defnd process-select-rows-from [obj]
  (l/infod src fn-name "obj" obj)
  (let [r (process-sql db/sql-select-rows-from obj)]
    ;; (l/infod src fn-name "r" r)
    r))

;; TODO LightTable: paredit grow right should jump over comment
(l/defnd process-show-tables-from [obj]
  (process-sql db/sql-show-tables-from obj))

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
          ;;        {:dbase dbase :table %1 :rows-displayed 2 :idx (u/kw-table %2)})
          ;;      tables
          ;;      (into [] (range count-tables)))
          ]
      (l/infod src fn-name "r" r)
      r)))

(l/defnd process-request [params idx]
  (let [table ((u/kw :table :name idx) params)
        r (encode-table {:table table :data (db/s params idx) :idx idx})]
    ;; (l/infod src fn-name "table" table)
    ;; (l/infod src fn-name "r" r)
    r))

(def fetch-fns {:select-rows-from           process-select-rows-from
                :show-tables-from           process-show-tables-from
                :show-tables-with-data-from process-show-tables-with-data-from
                :request                    process-request
                })

;; returns
;; (def rx
;;   {:row1 {:emp_no 10002 :birth_date "1964-06-01" :first_name "Bezalel" :last_name "Simmel"  :gender "F" :hire_date "1985-11-20"}
;;    :row0 {:emp_no 10001 :birth_date "1953-09-01" :first_name "Georgi"  :last_name "Facello" :gender "M" :hire_date "1986-06-25"}})
(l/defnd manipulate-rows [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (let [vals-vec (u/convert-to-korks u/kw-row data)
        r (first vals-vec)]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    ;; (l/infod src fn-name "r" r)
    r))

(l/defnd m-x-one [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (let [vals-vec (u/convert-to-korks u/kw-row data)
        r (first vals-vec)]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    ;; (l/infod src fn-name "r" r)
    r))

(l/defnd m-select-rows-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (let [
        ;; params [{:dbase "employees", :table "departments", :idx 0}])
        ;; data [[{:dept_no "d009", :dept_name "Customer Service"}
        ;;        {:dept_no "d005", :dept_name "Development"}]]
        rlist
        (doall (map (fn [p d]
                      (merge p
                             {:data (manipulate-rows p [d])
                              :row-count (db/row-count (:table p))}))
                    params data))
        rvec (into [] rlist)
        ks (into [] (map u/kw-table (range (count rvec))))
        r (zipmap ks rvec)
        ;; TODO extend-table must be done in the client.cljs
        ]
    ;; (l/infod src fn-name "rvec" rvec)
    ;; (l/infod src fn-name "r" r)
    r))

(l/defnd m-show-tables-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (m-select-rows-from params data))

(l/defnd m-show-tables-with-data-from [params data]
  ;; (l/infod src fn-name "params" params)
  ;; (l/infod src fn-name "data" data)
  (m-select-rows-from params data))

(l/defnd m-request [p]
  (into [] p))

(def manipulator-fns {:select-rows-from            m-select-rows-from
                      :show-tables-from            m-show-tables-from
                      :show-tables-with-data-from  m-show-tables-with-data-from
                      :request                     m-request
                      })

(l/defnd get-params-for-fetch [xhr-data]
  (l/infod src fn-name "xhr-data" xhr-data)
  (into []
        (for [kw-dbase (keys (get-in xhr-data [:data]))
              kw-table (keys (get-in xhr-data [:data kw-dbase :data]))]
          (let [
                ;;idx-kw-dbase idx-kw-table
                ;;kw-dbase           (nth (keys (get-in xhr-data [:data])) idx-kw-dbase)
                ;;idx-dbase          (subs (name kw-dbase) (count "dbase") (count (name kw-dbase)))
                ;;kw-table           (nth (keys (get-in xhr-data [:data kw-dbase :data])) idx-kw-table)
                ;;idx-table          (subs (name kw-table) (count "table") (count (name kw-table)))

                get-table-name     [:data kw-dbase :data kw-table :name]
                table-name         (get-in xhr-data get-table-name)

                get-dbase-name     [:data kw-dbase :name]
                dbase-name         (get-in xhr-data get-dbase-name)

                get-rows-displayed [:data kw-dbase :data kw-table :data :rows-displayed]
                rows-displayed     (get-in xhr-data get-rows-displayed)
                r {:dbase dbase-name :table table-name :rows-displayed rows-displayed :idx kw-table}
                ]
            ;; (l/infod src fn-name "kw-dbase" kw-dbase)
            ;; (l/infod src fn-name "idx-dbase" idx-dbase)
            ;; (l/infod src fn-name "kw-table" kw-table)
            ;; (l/infod src fn-name "idx-table" idx-table)
            (l/infod src fn-name "r" r)
            r)
          )))

(l/defnd get-params-for-select
  [{:keys [dbase entities column value] :as params}]
  (let [hm (for [[i e] (map-indexed vector entities)]
            {:dbase dbase :table e :rows-displayed 2 :idx (u/kw-table i) :column column :value value})
        r (into [] hm)]
    (l/infod src fn-name "r" r)
    r))

(l/defnd get-dbases
  [fetch-fn manipulator-fn params]
  (println src fn-name "fetch-fn" fetch-fn)
  (println src fn-name "manipulator-fn" manipulator-fn)
  (println src fn-name "params" params)
  (let [data (into [] (map fetch-fn params))]
    (println src fn-name "data" data)
    (let [r (manipulator-fn params data)]
      (println src fn-name "r" r)
      r)))

(l/defnd get-data
  [fetch-fn manipulator-fn params]
  ;; (l/infod src fn-name "fetch-fn" fetch-fn)
  ;; (l/infod src fn-name "manipulator-fn" manipulator-fn)
  (l/infod src fn-name "params" params)
  (let [data (into [] (map fetch-fn params))]
    (l/infod src fn-name "data" data)
    (let [r (manipulator-fn params data)]
      (l/infod src fn-name "r" r)
      r)))

(l/defnd request
  [{:keys [dbase column value] :as edn-params}]
  (l/infod src fn-name "edn-params" edn-params)
  (let [
        entities-wc    (db/entities-with-column {:entities db/all-entities :column column})

        fetch-fn       process-data-with-column-value
        manipulator-fn m-select-rows-from
        params         (get-params-for-select (into edn-params {:entities entities-wc}))
        ]
    (let [r (get-data fetch-fn manipulator-fn params)]
      r)))

(l/defnd fetch-dbases
  [edn-params]
  (let [
        kw-fetch-fn (:name edn-params)
        fetch-fn       (kw-fetch-fn fetch-fns)
        manipulator-fn (kw-fetch-fn manipulator-fns)
        ;; params (get-params-for-fetch edn-params)
        params [{:dbase "employees", :rows-displayed 4, :idx 0}]
        ]
    (println src fn-name "fetch-fn" fetch-fn)
    (println src fn-name "manipulator-fn" manipulator-fn)
    (println src fn-name "params" params)
    (let [r (get-dbases fetch-fn manipulator-fn params)
          ]
      r)))

(l/defnd fetch
  [edn-params]
  (let [;; TODO get the content of N-th key? this could be done better
        kw-fetch-fn (:name edn-params)

        fetch-fn       (kw-fetch-fn fetch-fns)
        manipulator-fn (kw-fetch-fn manipulator-fns)
        params         (get-params-for-fetch edn-params)
        ]
    (let [r (get-data fetch-fn manipulator-fn params)]
      r)))

;; clean the REPL - works only in clojure not in clojurescript
;; (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))
