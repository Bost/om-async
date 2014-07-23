(ns om-async.utils)

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

(defn convert-to-korks [f1 f2 vals-vec]
  (let [fn-name "convert-to-korks"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [xs (map (fn [x] (to-korks f1 x)) vals-vec)
          xs-seq (into [] xs)
          ys (to-korks f2 xs-seq)]
      ;; (l/infod src fn-name "xs" xs)
      ;; (l/infod src fn-name "xs-seq" xs-seq)
      ;; (l/infod src fn-name "ys" ys)
      ys)))

(defn row-vals-to-korks [vals-vec]
  (convert-to-korks kw-col kw-row vals-vec))

(defn col-vals-to-korks [vals-vec]
  (convert-to-korks kw-row kw-col vals-vec))


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


(def row-vec
  [
   [["t0-r0-c0" "t0-r0-c1" "t0-r0-c2"]
    ["t0-r1-c0" "t0-r1-c1" "t0-r1-c2"]]

;;    [["t1-r0-c0" "t1-r0-c1" "t1-r0-c2"]
;;     ["t1-r1-c0" "t1-r1-c1" "t1-r1-c2"]]

   ]
  )
(xconvert-to-korks :table kw-col kw-row row-vec)

;; (def row-vec [["row-0-col0" "row-0-col-1" "row-0-col2"]])
;; (row-vals-to-korks row-vec)
;; {:row0 {:col0 row-0-col0, :col1 row-0-col-1, :col2 row-0-col2}}

;; (def col-vec [["row-0-col0" "row-1-col-0" "row-2-col0"]])
;; (col-vals-to-korks col-vec)
;; {:col0 {:row0 row-0-col0, :row1 row-1-col-0, :row2 row-2-col0}}

;; (def row-vals ["Customer Service" "d009"])
;; (row-vals-to-korks row-vals)
;; (to-korks u/kw-col row-vals)
;; (def in-row-vals {:col0 "Customer Service", :col1 "d009"})
;; (def i in-row-vals)


