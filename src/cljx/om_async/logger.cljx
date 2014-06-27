(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
   ))

(def files ["client.cljs" "transform.clj"])
(def functions [
                "view"
                "construct-component"
                "onClick"
                "tr"
                "create-table-for-columns"
                "get-data"
                "create-table"
                "table-elem"
                "get-data"
                "get-table-data"
                "rows"
                ;; "process-request"
                ;; "fetch"
                ])

(defn infod [src fn-name def-name def-val]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; "
            "(def " def-name " " (pr-str def-val) ")")))))

(defn info [src fn-name msg]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println (str src "; " fn-name "; " msg)))))


(defmacro defnd
  [fname params & body]
  `(defn ~fname ~params
     (do
       (println "defnd")
       ~@body)))

(defnd f [x y z]
  (println "foo")
  (+ x y z))

(f 1 2 3)
