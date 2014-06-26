(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
   ))

(defn info [src fn-name msg]
  (if (u/contains-value?
       ["client.cljs"
        "transform.clj"
        ] src)
    (if (u/contains-value?
         ["construct-component"
          ;; "fetch"
          ;; "create-table-for-columns"
          "get-data"
          "create-table"
          "table-elem"
          "process-request"
          "view"] fn-name)
      (println (str src "; " fn-name "; " msg))
      )))
