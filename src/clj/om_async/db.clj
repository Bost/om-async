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

(defn dx []
  (sql/query db
             ["select * from employees where emp_no between 10001 and 10003"]
             ))

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
        (into []
              (map (fn [v]
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

(defn data []
    [(result (dx))])
