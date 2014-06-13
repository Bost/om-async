(ns om-async.db
  (:require [clojure.java.jdbc :as sql]))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})


(def t ["employees" "departments"])

(defn create-key [i]
  (keyword (str "col" i)))

(defn create-item [v i]
  {(create-key i)v})

(defn select-from [table]
  (sql/query db
             [(str "select * from " table " limit 4")]))

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

(defn data []
  (let [r
    [
     (result (select-from (nth t 0)))
     (result (select-from (nth t 1)))
     ]]
;;     (println r)
    r))

;; [{:col5 {:col-name [emp_no], :col-vals [10001 10002 10003 10004]},
;;   :col4 {:col-name [birth_date], :col-vals [1953-09-02 1964-06-02 1959-12-03 1954-05-01]},
;;   :col3 {:col-name [first_name], :col-vals [Georgi Bezalel Parto Chirstian]},
;;   :col2 {:col-name [last_name], :col-vals [Facello Simmel Bamford Koblick]},
;;   :col1 {:col-name [gender], :col-vals [M F M M]},
;;   :col0 {:col-name [hire_date], :col-vals [1986-06-26 1985-11-21 1986-08-28 1986-12-01]}}

;;  {:col1 {:col-name [dept_no], :col-vals [d009 d005 d002 d003]},
;;   :col0 {:col-name [dept_name], :col-vals [Customer Service Development Finance Human Resources]}}]
