(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  (:require [om-async.utils :as u]
            ;; [taoensso.timbre :as logger]
            )
;;   (:use [clojure.walk :as walk])
   )

(def src "logger.cljx")

(def files
  {:client.cljs [ "table"
                  "view"
                  "construct-component"
;;                   "table"
;;                   "init"
;;                   "color"
;;                   "tr"
;;                   "view-onComplete"
;;                   "onClick-onComplete"
;;                   "onClick"
;;                   "defcomponent-td"
;;                   "defcomponent-table-controller"
                  "render-multi"
                  "ks-other"
                  "render"
                  "get-display"
                  "table-sorter"
                  "new-to-old"
                  "view-:on-complete"
                  ]
   :onclick.cljs [
                  "toggle-table"
;;                   "displayed-rows"
;;                   "edn-xhr"
;;                   "activate-:on-complete"
;;                   "activate"
                  ]
   :cli_transform.cljs [
;;                         "extend-table"
;;                         "extend-map"
;;                         "xtable"
;;                         "extend-all"
                        ]
   :transform.clj [
;;                    "get-params-for-fetch"
;;                    "process-data-with-column-value"
                   "fetch"
;;                    "request"

;;                    "encode-table"
;;                    "process-select-rows-from"
;;                    "m-show-tables-from"
;;                    "m-select-rows-from"
;;                    "m-select-rows-from-new"
;;                    "m-show-tables-with-data-from"
;;                    "process-show-tables-with-data-from"
;;                    "process-sql"
                   ]
   :server.clj [
;;                 "routes-PUT-select-id"
;;                 "routes-PUT-fetch"
                ]
   :utils [
;;            "convert-to-korks"
           ]

   :db.clj [
;;             "entities-with-colum"
;;             "data-with-column-value"
;;             "calc-row-limit"
;;             "sql-show-tables-from"
;;             "sql-select-rows-from"
;;                "fn-name"
            ]
   }
  )

;; TODO use (partial ..)
(defn infod [src fn-name def-name def-val]
;;   (println "src" src)
;;   (println "fn-name" fn-name)
;;   (println "def-name" def-name)
;;   (println "def-val" def-val)
  (let [fns ((keyword src) files)]
    (if fns
      (if (u/in? fns fn-name)
        (println
         (str src "; " fn-name "; "
              "(def " def-name " " (pr-str def-val) ")"))))
    def-val))

;; TODO macro: choose between clojure.pprint/pprint (clj) & println (cljs)
;; TODO see (JSON/stringify obj nil 2)
(defn info [src fn-name msg]
;;   (println "src" src)
;;   (println "fn-name" fn-name)
;;   (println "msg" msg)
;;   (println "files" files)
  (if (u/contains-value? ((keyword src) files) fn-name)
    (if true ;; (u/contains-value? functions fn-name)
      (println
       (str src "; " fn-name "; " msg)))))

(defn warn [src fn-name msg]
  (println
   (str "WARN " src "; " fn-name "; " msg)))

(keyword "client.cljs")

;; (defn error [src fn-name msg]
;;   (let [separator "========="]
;;     (info src fn-name separator)
;;     (info src fn-name (str "ERROR: " msg))
;;     (info src fn-name separator)))

(defn encode-name-val [n v]
  (str "(def " n " "
       (if (string? v)
         (str "\"" v "\"")
         v) ")"))

;; (defmacro defnd
;;   "This macro is translated to clj file. In cljs files
;;   (:require-macros [om-async.logger :as l]) and then l/defnd
;;   must be used."
;;   [fname params & body]
;;   `(defn ~fname ~params
;;      (do
;;        (println
;;         (map #(str ~'src "; " '~fname "; "
;;                    (encode-name-val %1 %2) "\n")
;;              '~params ~params))
;;        ~@body)))

(defmacro defnd
  "This macro is translated to clj file. In cljs files
  (:require-macros [om-async.logger :as l]) and then l/defnd
  must be used."
  [fname params & body]
  `(defn ~fname ~params
     (let [~'fn-name (str '~fname)]
       ~@body)) )

;; (macroexpand '(defnx foo [x y z]
;;                 (+ x y z)))
;; (defnd f [x y z]
;;   (println "defnd f:"
;;            (+ x y z)))

;; (f 1 2 3)

;; ;; (macroexpand '(defnd f [x y z]
;; ;;   (println "foox")
;; ;;   (+ x y z)))

