(ns om-async.utils)

;; TODO create reasonable logging function!

(defn create-keyword [key-name key-index]
  (keyword (str key-name key-index)))

(defn column-keyword [key-index]
  (create-keyword "col" key-index))

(defn table-keyword [key-index]
  (create-keyword "table" key-index))

