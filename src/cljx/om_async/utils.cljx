(ns om-async.utils)

(def prefixes {:table "table" :dbase "dbase" :col "col" :row "row"})
(def modifiers {:name "Name" :val "Val"})

(defn kw [prefix modifier idx]
  (keyword (str (prefix prefixes) (modifier modifiers) idx)))
