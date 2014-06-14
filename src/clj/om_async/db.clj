(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            [om-async.utils :as utils]
            ))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

;; TODO put data to a hash-map {:table0 data0 :table1 data1 ...}
(defn create-item [v i]
  {(utils/column-keyword i) v})

(defn table-cols [raw-data]
  (let [vx (into [] raw-data)
        ;; since the table structure is the same operate only on the first row
        ;; TODO modify the select with 'fetch first 1 row only'
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

(defn result [raw-data]
  (apply merge
         (let [all-vals (table-vals raw-data)
               all-cols (table-cols raw-data)
               indexes (range 0 (count all-cols))
               table-vals (map #(nth-from all-vals %) indexes)]
           (map (fn [i c v]
                  {(utils/column-keyword i) {:col-name c :col-vals v}})
                indexes
                all-cols
                table-vals))))

(def e "employees")
(def d "departments")
(def t ["employees" "departments"])

;;;;;;;;;;;
(defn select-rows-from [table]
  (sql/query db
             [(str "select * from " table " limit 1")]))

(defn select-rows-from-processor [table]
  ;; (println (str "select-rows-from-processor: (result (select-rows-from \"" table "\"))"))
  (result (select-rows-from table)))


(map select-rows-from-processor t)

(
 {:col1 {:col-name ["gender"], :col-vals ["M"]},
  :col0 {:col-name ["hire_date"], :col-vals ["1986-06-26"]}}

 {:col1 {:col-name ["dept_no"], :col-vals ["d009"]},
  :col0 {:col-name ["dept_name"], :col-vals ["Customer Service"]}}
 )
;;;;;;;;;;;


;;;;;;;;;;;
(defn show-tables-from [db-name]
  (println (str "Fetching data from dbase: " db-name))
  (sql/query db
             [(str "SHOW TABLES FROM " db-name)]))
(def show-tables-from-memo (memoize show-tables-from))

(defn show-tables-from-processor [db-name]
  ;; (println (str "show-tables-from-processor: (" (name 'show-tables-from-memo) " \"" db-name"\")"))
  (result
   (show-tables-from-memo db-name)))
;;;;;;;;;;;

;;;;;;;;;;;
(defn show-tables-with-data-from-processor [db-name]
  ;; (println (str "show-tables-with-data-from-processor: (" (name 'show-tables-from-memo) " " db-name")"))
  (let [list-tables (map first (table-vals (show-tables-from-memo db-name)))
        tables (into [] list-tables)]
    ;; (println (str "(map " (name 'select-rows-from-processor) " " tables ")"))
    (map select-rows-from-processor tables)))
;;;;;;;;;;;

(def fetch-fns {:select-rows-from           select-rows-from-processor
                :show-tables-from           show-tables-from-processor
                :show-tables-with-data-from show-tables-with-data-from-processor
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
    (manipulator-fn (map #(fetch-fn %) params))))
