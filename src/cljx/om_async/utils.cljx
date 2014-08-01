(ns om-async.utils)

(def src "utils")

(def prefixes {:dbase "dbase" :table "table" :row "row" :col "col"})
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


(defn kw-dbase [idx]
  (kw-prefix :dbase idx))

(defn kw-table [idx]
  (kw-prefix :table idx))

(defn kw-row [idx]
  (kw-prefix :row idx))

(defn kw-col [idx]
  (kw-prefix :col idx))


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

(defn to-korks [f values]
  (let [r (map-indexed (fn [idx value] {(f idx) value}) values)
        rv (into [] r)]
    (apply merge rv)))

(defn convert-to-korks-orig [f1 f2 vals-vec]
  (let [fn-name "convert-to-korks-orig"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [xs (map (fn [x] (to-korks f1 x)) vals-vec)
          xs-seq (into [] xs)
          ys (to-korks f2 xs-seq)]
      ;; (l/infod src fn-name "xs" xs)
      ;; (l/infod src fn-name "xs-seq" xs-seq)
      ;; (l/infod src fn-name "ys" ys)
      ys)))

(defn convert-to-korks [f1 vals-vec]
  (let [fn-name "convert-to-korks"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [xs (map (fn [x] (to-korks f1 x)) vals-vec)
          xs-seq (into [] xs)
          ;; ys (to-korks f2 xs-seq)
          ]
      ;; (l/infod src fn-name "xs" xs)
      ;; (l/infod src fn-name "xs-seq" xs-seq)
      xs)))

(def data [[{:dept_name "Customer Service", :dept_no "d009"}
            {:dept_name "Development", :dept_no "d005"}]])

(convert-to-korks kw-row data)

(defn row-vals-to-korks [vals-vec]
  (convert-to-korks-orig kw-col kw-row vals-vec))

(defn col-vals-to-korks [vals-vec]
  (convert-to-korks-orig kw-row kw-col vals-vec))


(defn xconvert-to-korks [f3 f1 f2 vals-vec]
  (let [fn-name "convert-to-korks"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [xs (map (fn [x] (to-korks f1 x)) vals-vec)
          xs-seq (into [] xs)
          ys (to-korks f2 xs-seq)
          zs {f3 ys}]
      ;; (l/infod src fn-name "xs" xs)
      ;; (l/infod src fn-name "xs-seq" xs-seq)
      ;; (l/infod src fn-name "ys" ys)
      ;; (l/infod src fn-name "zs" zs)
      zs)))
