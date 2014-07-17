(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
            )
;;   (:use [clojure.walk :as walk])
   )

(def src "logger.cljx")

(def files
  ["client.cljs" "transform.clj"]
;;   ["client.cljs"]
;;   ["transform.clj"]
  )
(def functions [
;;                 "view"
                "construct-component"
;;                 "onClick"
;;                 "tr"
;;                 "create-table-for-columns"
;;                 "get-data"
;;                 "create-table"
;;                 "table-elem"
;;                 "table-vals"
;;                 "get-data"
;;                 "get-table-data"
;;                 "rows"
                "convert-to-korks"
                "encode-table"
;;                 "f"
                ;; "process-request"
                "fetch"
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


;; (defmacro defnd
;;   "This macro is translated to clj file. In cljs files
;;   (:require-macros [om-async.logger :as l]) and then l/defnd
;;   must be used."
;;   [fname params & body]
;;   `(defn ~fname ~params
;;      (do
;;        (info ~'src '~fname "msgx")
;;        ~@body)))

;; (defnd f [x y z]
;;   (println "defnd f:"
;;            (+ x y z)))

;; (f 1 2 3)

;; ;; (macroexpand '(defnd f [x y z]
;; ;;   (println "foox")
;; ;;   (+ x y z)))

