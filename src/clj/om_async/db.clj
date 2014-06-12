(ns om-async.db
  (:require [clojure.java.jdbc :as sql]))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

(defn dx []
  (sql/query db
             ["select * from employees where emp_no between 10001 and 10002"]
             ))

(def x (dx))

(def vx (into [] x))

(def row (nth vx 0))

(def k (keys row))

(def kv (into [] k))

(def n (map #(name %) kv))

(def nv (into [] n))

(def vvn (into [] (map (fn [v] [v]) nv)))

(def cols (map (fn [v] {:col-name v :col-vals ["x1" "x2"]}) vvn))

(def vcols (into [] cols))

(defn create-item [v i]
  {
   (keyword (str "col" i))
   v}
  )

(def c1 (create-item "val" 1))
(def c2 (create-item "val" 2))

(merge c1 c2)

;; TODO start to iterate from 0
(def all-cols
  (map #(create-item %1 %2)
       vcols
       (iterate inc 1)))

(def v-all-cols (into [] all-cols))

(def result (apply merge v-all-cols))
;; {:col6 {:col-name ["emp_no"], :col-vals ["x1" "x2"]},
;;  :col5 {:col-name ["birth_date"], :col-vals ["x1" "x2"]},
;;  :col4 {:col-name ["first_name"], :col-vals ["x1" "x2"]},
;;  :col3 {:col-name ["last_name"], :col-vals ["x1" "x2"]},
;;  :col2 {:col-name ["gender"], :col-vals ["x1" "x2"]},
;;  :col1 {:col-name ["hire_date"], :col-vals ["x1" "x2"]}
;;  }


result
(defn data []
[
;;  {
;;   :col1 {:col-name ["xdbAnimals"] :col-vals ["Lion" "Zebra" "Buffalo" "Antelope"]}
;;   :col2 {:col-name ["Names"] :col-vals ["Jim" "Jack" "Fred" "Marie"]}
;;   :col3 {:col-name ["Tech"] :col-vals ["Clojure" "Java" "Python" "Perl"]}
;;   }
 result
;; {:col6 {:col-name ["emp_no"], :col-vals ["x1" "x2"]},
;;  :col5 {:col-name ["birth_date"], :col-vals ["x1" "x2"]},
;;  :col4 {:col-name ["first_name"], :col-vals ["x1" "x2"]},
;;  :col3 {:col-name ["last_name"], :col-vals ["x1" "x2"]},
;;  :col2 {:col-name ["gender"], :col-vals ["x1" "x2"]},
;;  :col1 {:col-name ["hire_date"], :col-vals ["x1" "x2"]}
;;  }
 ]
  )

;; (data)
;; [
;;  {:hire_date #inst "1986-06-25T22:00:00.000-00:00",
;;  :gender "M",
;;  :last_name "Facello",
;;  :first_name "Georgi",
;;  :birth_date #inst "1953-09-01T23:00:00.000-00:00",
;;   :emp_no 10001}

;; {:hire_date #inst "1985-11-20T23:00:00.000-00:00",
;;  :gender "F",
;;  :last_name "Simmel",
;;  :first_name "Bezalel",
;;  :birth_date #inst "1964-06-01T23:00:00.000-00:00",
;;  :emp_no 10002}
;; ]


(defn foo []
  (println "Hello, from clojure!"))
