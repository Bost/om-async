(ns om-async.logger
  ;; TODO find a logger for server (clj) and client (cljs)
  ;; (:require [taoensso.timbre :as logger])
  )

(defn info [file fn-name msg]
  (println (str file "; " fn-name "; " msg)))
