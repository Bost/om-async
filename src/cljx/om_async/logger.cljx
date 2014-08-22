(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
            )
;;   (:use [clojure.walk :as walk])
   )

(def src "logger.cljx")

(def files
  ["client.cljs" "transform.clj" "db.clj" "utils"]
;;   ["client.cljs"]
;;   ["transform.clj"]
;;   nil
  )

(def functions
  [
   ;;;; client.cljs
;;    "view"
;;    "create-table-for-columns"
;;    "get-data"
;;    "create-table"
;;    "table-elem"
;;    "get-data"
;;    "convert-to-korks"
;;    "f"
;;    "construct-component"
;;    "table-elem"
;;    "render-data-vec"
;;    "render-data"
;;    "render-table"
;;    "render-multi"
   "render-row"
;;    "init"
;;    "render"
   "render-td"
;;    "onClick"
;;    "tr"

   ;;;; utils
;;    "convert-to-korks"
   ])

(defn infod [src fn-name def-name def-val]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; "
            "(def " def-name " " (pr-str def-val) ")")))))

;; TODO macro: choose between clojure.pprint/pprint (clj) & println (cljs)
;; TODO see (JSON/stringify obj nil 2)
(defn info [src fn-name msg]
  (if (u/contains-value? files src)
    (if (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; " msg)))))

(defn error [src fn-name msg]
  (let [separator "=========="]
    (info src fn-name separator)
    (info src fn-name (str "ERROR: " msg))
    (info src fn-name separator)))

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

