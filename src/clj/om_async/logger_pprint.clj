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
   "fetch"
;;    "encode-table"
;;    "convert-val"
;;    "m-select-rows-from"
;;    "m-x"

   ;;;; db.clj
;;    "sql-select-rows-from"

   ;;;; utils
;;    "convert-to-korks"
   ])
(defn info [src fn-name msg]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (binding [*print-length* 130]
        (clojure.pprint/pprint
         (str src "; " fn-name "; " msg))))))

(defn infod [src fn-name def-name def-val]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; "
            "(def " def-name " "
            (binding [*print-length* 130]
              (clojure.pprint/pprint def-val)) ")")))))
