(ns om-async.cli-transform
  (:require [om-async.logger :as l])
  (:require-macros [om-async.logger :as l]))

(def src "cli_transform.cljs")

;; ;; "Turns {:a 1, :b 2} to {:a {:val 1, :active false}, :b {:val 2, :active false}}"
;; "Turns {:a 1, :b 2} to {:a {:val 1}, :b {:val 2}}.
;; Seems like the :active is not needed because of local component changing using
;; (om/set-state! owner ks (not active))"
(l/defnd extend-map [m]
  (l/infod src fn-name "m" m)
  (reduce-kv (fn [m k v]
               (assoc m k {:val v
                           ;; :active false
                           }))
             {} m))

(defn ff [m k v] (assoc m k (extend-map v)))

(l/defnd extend-table [t]
  ;;(l/infod src fn-name "t" t)
  (let [rows (get-in t [:data])]
    ;;(l/infod src fn-name "rows" rows) ;; (type rows) => om.core/MapCursor
    (l/infod src fn-name "rows" rows)
    (l/infod src fn-name "(type rows)" (type rows))
    (let [ff-rows (reduce-kv ff {} rows)]
      ;; (l/infod src fn-name "ff-rows" ff-rows)
      (let [r (assoc t :data ff-rows)]
        ;; (l/infod src fn-name "r" r)
        r))))

(l/defnd xtable [tfull k]
  (l/infod src fn-name "tfull" tfull)
  (l/infod src fn-name "k" k)
  (let [rlist (extend-table (get-in tfull [k]))
        r [rlist]]
    ;; (l/infod src fn-name "rlist" rlist)
    (l/infod src fn-name "r" r)
    r))

(l/defnd extend-all [tfull]
  (l/infod src fn-name "tfull" tfull)
  (let [all-xtable (for [k (keys tfull)]
                     (xtable tfull k))
        all-tables-data (for [t (apply concat all-xtable)]
                          (dissoc t :dbase))
        indexes (for [d all-tables-data]
                  (:idx d))
        indexed-tables (for [[i d] (map vector indexes all-tables-data)]
                         {i d})

        dbase-data {:dbase "employees"
                    :data (into {}
                                (apply concat indexed-tables))}
        ]
    (l/infod src fn-name "all-xtable" all-xtable)
    ;; (println "dbase-data" dbase-data)
    dbase-data))
