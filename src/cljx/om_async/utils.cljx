(ns om-async.utils)

(def prefixes {:table "table" :dbase "dbase" :col "col" :row "row"})
;; (def modifiers {:name "Name" :val "Val"})
(def modifiers {:name "N" :val "V"})

(defn kw [prefix modifier idx]
  (keyword (str (prefix prefixes)
                (if (nil? modifier) nil (modifier modifiers))
                idx)))

(def d  "departments")
(def e  "employees")
(def s  "salaries")
(def de "dept_emp")
(def dm "dept_manager")
(def t  "titles")


(def t ["employees" "departments"])
