(ns om-async.db
  (:require [clojure.java.jdbc :as sql]
            [om-async.utils :as utils]
            ;; [taoensso.timbre :as logger]
            ))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

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

(def e "employees")
(def d "departments")
(def t ["employees" "departments"])

;;;;;;;;;;;
(defn select-rows-from [table]
  (let [sql-cmd (str "select * from " table " limit 2")]
    (println (str "db.clj; select-rows-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str "db.clj; r: " r))
      r)))

(defn select-rows-from-processor [table]
  ;; (println (str "select-rows-from-processor: (result (select-rows-from \"" table "\"))"))
  (result (select-rows-from table)))

(select-rows-from-processor d)
;;;;;;;;;;;


;;;;;;;;;;;
(defn show-tables-from [db-name]
  ;; (println (str "show-tables-from: Fetching data from dbase: " db-name))
  (let [sql-cmd (str "SHOW TABLES FROM " db-name)]
    (println (str "db.clj; show-tables-from: " sql-cmd))
    (let [r (sql/query db [sql-cmd])]
      ;; (println (str "db.clj; r: " r))
      r)))

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
    ;; (println (str "db.clj; (map " (name 'select-rows-from-processor) " " tables ")"))
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
        (let [data (manipulator-fn (map #(fetch-fn %) params))]
          ;; (println "data: " data)
          data)))
