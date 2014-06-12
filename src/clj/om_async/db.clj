(ns om-async.db
  (:require [clojure.java.jdbc :as sql]))

(def db {:classname "com.mysql.jdbc.Driver"
         :subprotocol "mysql"
         :subname "//127.0.0.1:3306/employees"
         :user "root"
         :password "bost"})

(defn print-users []
  (sql/query db
             ["select * from employees where emp_no between 10001 and 10002"]
             ))

;; (print-users)
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
