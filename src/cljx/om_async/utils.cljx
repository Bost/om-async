(ns om-async.utils)

(def prefixes {:table "table" :dbase "dbase" :col "col" :row "row"})
;; (def modifiers {:name "Name" :val "Val"})
(def modifiers {:name "N" :val "V"})

(defn kw [prefix modifier idx]
  (keyword (str (if (nil? prefix)   nil (prefix   prefixes))
                (if (nil? modifier) nil (modifier modifiers))
                idx)))

(defn kw-prefix [prefix idx]
  (kw prefix nil idx))

(defn kw-name [idx]
  (kw nil :name idx))

(defn kw-val [idx]
  (kw nil :val idx))

(defn contains-value?
  "'contains?' tests if the numeric key is within the range of indexes.
  We need to use 'some'."
  [coll element]
  (boolean (some #(= element %) coll)))

(def d  "departments")
(def e  "employees")
(def s  "salaries")
(def de "dept_emp")
(def dm "dept_manager")
(def t  "titles")

(def t ["employees" "departments"])

