(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
   ))

(def files ["client.cljs" "transform.clj"])
(def functions ["construct-component"
                ;; "fetch"
                ;; "create-table-for-columns"
                "get-data"
                "create-table"
                "table-elem"
                "process-request"
                "view"])

(defn infod [src fn-name def-name def-val]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (let [surr (if (string? def-val) "\"" "")]
        (println
         (str src "; " fn-name "; "
              "(def " def-name " " surr def-val surr ")"))))))

(defn info [src fn-name msg]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println (str src "; " fn-name "; " msg)))))
