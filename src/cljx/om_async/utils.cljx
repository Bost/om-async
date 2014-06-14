(ns om-async.utils)

(defn create-keyword [key-name key-index]
  (keyword (str key-name key-index)))

(defn column-keyword [key-index]
  (create-keyword "col" key-index))

