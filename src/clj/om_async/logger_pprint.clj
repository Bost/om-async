(ns om-async.logger-pprint
  (:require [om-async.utils :as u]
            ))

;; Transformation layer between Ring and DB access functions here.

(def src "logger-pprint.clj")

(def files
  ["transform.clj" "db.clj"]
;;   ["transform.clj"]
;;   nil
  )

(def functions
  [
   ;;;; "transform.clj"
;;    "extend-table"
;;    "xtable"
;;    "extend-all"
;;    "process-request"
;;    "process-sql"
;;    "process-select-rows-from"
;;    "process-show-tables-with-data-from"
;;    "fetch"
;;    "encode-table"
;;    "convert-val"
;;    "m-select-rows-from"
;;    "m-x"

   ;;;; db.clj
;;    "sql-select-rows-from"

   ;;;; utils
;;    "convert-to-korks"
   ])

(defn print-fn [& k]
  (let [k (if (nil? k) :p :pp)]
    (k {:p println :pp clojure.pprint/pprint})))

(defn info
  "TODO test if print-fn is either println or clojure.pprint/pprint"
  [src fn-name msg & print-k]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (binding [*print-length* 130]
        ((print-fn print-k)
         (str src "; " fn-name "; " msg))))))

(defn infod [src fn-name def-name def-val & print-k]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; "
            "(def " def-name " "
            (binding [*print-length* 130]
              ((print-fn print-k) def-val)) ")")))))
