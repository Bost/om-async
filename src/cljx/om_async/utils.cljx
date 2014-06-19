(ns om-async.utils)

;; TODO unify *-name and *-val keyword creation

(defn kw [prefix modifier idx]
  (keyword (str prefix modifier idx)))

(defn kw-name [prefix idx]
  (kw prefix "Name" idx))

(defn kw-val [prefix idx]
  (kw prefix "Val" idx))



(defn column-name-kw [idx]
  (kw-name "col" idx))

(defn column-val-kw [idx]
  (kw-val "col" idx))



(defn table-name-kw [idx]
  (kw-name "table" idx))

(defn table-val-kw [idx]
  (kw-val "table" idx))



(defn dbase-name-kw [idx]
  (kw-name "dbase" idx))

(defn dbase-val-kw [idx]
  (kw-val "dbase" idx))
