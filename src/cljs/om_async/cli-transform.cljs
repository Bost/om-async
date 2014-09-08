(ns om-async.cli-transform
;;   (:require [cljs.reader :as reader]
;;             [goog.events :as events]
;;             [goog.dom :as gdom]
;;             [om.core :as om :include-macros true]
;;             [om.dom :as dom :include-macros true]
;;             [cljs.core.async :refer [put! chan <!]]
;;             [om-async.utils :as u]
;;             [om-async.logger :as l]
;;             ;;[clojure.walk :as walk]
;;             )
;;   (:import [goog.net XhrIo]
;;            goog.net.EventType
;;            [goog.events EventType])
  (:require-macros [om-async.logger :as l]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; {
;; this part belongs to client.cljs

(defn extend-map [m]
  ;; "Turns {:a 1, :b 2} to {:a {:val 1, :active false}, :b {:val 2, :active false}}"
  "Turns {:a 1, :b 2} to {:a {:val 1}, :b {:val 2}}.
  Seems like the :active is not needed because of local component changing using
  (om/set-state! owner ks (not active))"
  (reduce-kv (fn [m k v]
               (assoc m k {:val v
                           ;; :active false
                           }))
             {} m))

(defn ff [m k v] (assoc m k (extend-map v)))

(l/defnd extend-table [t]
  (l/infod src fn-name "t" t)
  (let [rows (get-in t [:data])]
    ;;(l/infod src fn-name "rows" rows) ;; (type rows) => om.core/MapCursor
    (l/infod src fn-name "rows" rows)
    ;;(l/infod src fn-name "(type rows)" (type rows))
    (let [ff-rows (reduce-kv ff {} rows)]
      ;; (l/infod src fn-name "ff-rows" ff-rows)
      (let [r (assoc t :data ff-rows)]
        ;; (l/infod src fn-name "r" r)
        r))))

(l/defnd xtable [tfull k]
  ;; (l/infod src fn-name "tfull" tfull)
  ;; (l/infod src fn-name "k" k)
  (let [rlist (extend-table (get-in tfull [k]))
        r [rlist]]
    ;; (l/infod src fn-name "rlist" rlist)
    ;; (l/infod src fn-name "r" r)
    r))

(l/defnd extend-all [tfull]
  (let [ks (into [] (keys tfull))
        rks (map #(xtable tfull %) ks)
        r (into [] (apply concat (into [] rks)))
        ]
    ;; (l/infod src fn-name "r" r)
    r))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; }

