(ns om-async.utils)

;; TODO unify *-name and *-data keyword creation

(defn create-keyword [key-name key-index]
  (keyword (str key-name key-index)))

(defn column-keyword [key-index]
  (create-keyword "col" key-index))



(defn column-name-keyword [key-index]
  (create-keyword "col-name" key-index))

(defn column-data-keyword [key-index]
  (create-keyword "col-data" key-index))




(defn table-data-keyword [key-index]
  (create-keyword "table-data" key-index))

(defn table-name-keyword [key-index]
  (create-keyword "table-name" key-index))




(defn dbase-data-keyword [key-index]
  (create-keyword "dbase-data" key-index))

(defn dbase-name-keyword [key-index]
  (create-keyword "dbase-name" key-index))


