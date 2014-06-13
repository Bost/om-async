(ns om-async.db
  (:require [clojure.java.jdbc :as sql]))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

(defn create-key [i]
  (keyword (str "col" i)))

(defn create-item [v i]
  {(create-key i)v})

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
                  {(create-key i) {:col-name c :col-vals v}})
                indexes
                all-cols
                table-vals))))

(defn select-rows-from [table]
  (sql/query db
             [(str "select * from " table " limit 1")]))

(def t ["employees" "departments"])

(defn show-tables-from [db-name]
  (println "Fetching data...")
  (sql/query db
             [(str "SHOW TABLES FROM " db-name)]))
(def show-tables-from-memo (memoize show-tables-from))

(def fetch-fns
  {:show-tables-from show-tables-from-memo
   :select-rows-from select-rows-from
   :show-tables-with-data-from show-tables-with-data-from
   })

(defn fetch-data [fetch-fn params]
  (into []
        (map #(result (fetch-fn %)) params)))

(defn show-tables-with-data-from [db-name]
  (let [list-tables (map first (table-vals (show-tables-from-memo db-name)))
        tables (into [] list-tables)
        tables t]
    (fetch-data show-tables-from tables)))

(defn fetch [edn-params]
  (let [kw-fetch-fn (nth (keys edn-params) 0)
        fetch-fn (kw-fetch-fn fetch-fns)
        params (kw-fetch-fn edn-params)]
    (fetch-data fetch-fn params)))
