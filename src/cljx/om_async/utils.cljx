(ns om-async.utils)

(def prefixes {:table "table" :dbase "dbase" :col "col" :row "row"})
;; (def modifiers {:name "Name" :val "Val"})
(def modifiers {:name "N" :val "V"})

(defn kw [prefix modifier idx]
  (keyword (str (prefix prefixes)
                (if (nil? modifier) nil (modifier modifiers))
                idx)))

(def e "employees")
(def d "departments")
(def s "salaries")
(def t ["employees" "departments"])
