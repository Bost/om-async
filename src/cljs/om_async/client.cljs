(ns om-async.client
;;   (:use [jayq.core :only [$ css html document-ready]])
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [om.dom :as orig-dom :include-macros true]
            ;; this is probably not needed at the moment
            [cljs.core.async :as async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.onclick :as oc]
            [om-async.cli-transform :as t]
            )
  (:import [goog.net XhrIo]
           [goog.ui TableSorter])
  (:require-macros [om-async.logger :as l]
                   ;; defschema is needed by defcomponent
                   [schema.macros :refer [defschema]]))

(def src "client.cljs")

(enable-console-print!)

;; The 'client dbase'. swap! or reset! on app-state trigger root re-rendering
(def app-state (atom {}))

;; TODO consider using take-while/drop-while to increase performance
(defn column-filter? [elem-idx] true) ;; no elem is filtered out
;; (let [m {:a 1 :b 2 :c 1}]
;;   (select-keys m (for [[k v] m :when (= v 1)] k)))

(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no elem is filtered out
;; (defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no elem is filtered out

(l/defnd get-css
  [{:keys [default] :as params} owner]
  (let [ks (oc/full-ks params)
        active (om/get-state owner ks)
        r (if active "active" default)]
    ;; (om/transact! app ks (fn [] (not active)))
    r))

(l/defnd td
  [{:keys [cell idx-row column] :as params} owner]
  (let [td-val (get-in cell [:val])
        ;; TODO walking over the data from the server doesn't work properly
        p (into params {:ks-data [:data idx-row column] :kw-active [:active]})]
;;     (l/infod src fn-name "ks-data" ks-data)
;;     (l/infod src fn-name "cell" cell)
    (dom/td {:class (get-css p owner)
              :onClick (fn [e] (oc/activate (into p {:column column :elem-val td-val}) owner))}
            td-val)))

(l/defnd tr
  [{:keys [css row columns] :as params} owner]
  (let [ column nil ]
;;     (println "row: " row)
;;     (println "columns: " columns)
    (dom/tr {:class css}
            (for [col-kw (keys row)]
;;               (println "col-kw: " col-kw)
              (td (into params {:cell (col-kw row) :column col-kw}) owner)
              ))))

(l/defnd render-row
  [{:keys [rows] :as params} owner]
;;   (println "rows: " rows)
;;   (println "(keys rows): " (keys rows))
  (for [[idx-row row-kw] (map-indexed vector (keys rows))]
;;     (println "row: " row)
;;     (println "idx-row: " idx-row)
    (tr (into params {:css ""
                      :row (row-kw rows)
                      :idx-row idx-row
                      }) owner)
    ))

(l/defnd get-display
  [idx owner]
;;   (l/infod src fn-name "idx" idx)
  (let [display (om/get-state owner [idx :display])]
    (if (or (nil? display) display)
      #js {}
      #js {:display "none"})))

(defcomponent table-c
  [{:keys [header rows table-id] :as params} owner]
  (render-state [_ state]
;;                 (println "header: " header)
;;                 (println "table-id: " table-id)
;;                 (println "rows: " rows)
                (dom/table {:id table-id
                            :style (get-display (keyword table-id) owner)}
                           (dom/thead
                            (dom/tr
                             (for [h header]
                               (dom/th (str h)))))
                           (dom/tbody
                            (render-row {:rows rows :columns header} owner))))
  (did-mount [_]
             ;; (table-sorter owner table-id)
             ;; TODO consider using a map defining {:column sort-type} created by the server
             (let [component (TableSorter.)
                   alphaSort goog.ui.TableSorter.alphaSort
                   numericSort goog.ui.TableSorter.numericSort
                   reverseSort (goog.ui.TableSorter.createReverseSort numericSort)]
               (.decorate component (om/get-node owner))
               (.setSortFunction component 0 alphaSort)
               (.setSortFunction component 1 reverseSort))))

;; TODO rename table to extended-table (i.e. table with its table-control buttons)
(defcomponent table-controler [app owner]
  (render-state [_ state]
                (dom/div
                 (for [table app]
                   (let [dbname (get-in table [:dbase])
                         tname (get-in table [:table])
                         full-tname (str dbname "." tname)
                         data (get-in table [:data])

                         header (into [] (keys (:row0 data)))
                         buttons [{:name "more-rows" :fnc inc :exec-fnc? (fn [_] true)}
                                  {:name "less-rows" :fnc dec :exec-fnc? (fn [cnt-rows] (> cnt-rows 0))}]
                         ]
                     (dom/div {:id (str "div-" (name (:idx table)))}
                              tname
                              (dom/button {:onClick (fn [e]
                                                      (oc/toggle-table {:idx (:idx @table)}))}
                                          "toggle-table")
                              (dom/span {:id (str "span-" (name (:idx table)))}
                                        (for [button buttons]
                                          (dom/button {:ref "foo"
                                                       :onClick (fn [e]
                                                                  (oc/displayed-rows {:dbase (:dbase @table)
                                                                                      :table (:table @table)
                                                                                      :rows-displayed (:rows-displayed @table)
                                                                                      :idx (:idx @table)
                                                                                      :fnc (:fnc button)
                                                                                      :exec-fnc? (:exec-fnc? button)
                                                                                      }))}
                                                      (:name button))))

                              (om/build table-c {:header header
                                                 :rows data
                                                 :table-id (name (:idx table))
                                                 })))))))

(defcomponent render-multi [app owner]
  (render-state [_ state]
                (if (zero? (count app))        ;; TODO get rid of 'if'
                  (dom/div {:id "fetching"} "Fetching data...")
                  (dom/div {:id "main"}
                           (dom/button {:onClick (fn [e] (oc/deactivate-all app owner))} "deactivate-all")
                           (om/build table-controler app)))))

(defcomponent view
  ;; "data - application state data (a cursor); owner - backing React component
  ;; returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
  (will-mount [_]
              ;;(l/info src fn-name "will-mount")
              (oc/edn-xhr
               {:method :put
                :url "fetch"
                ;; TODO the idx should be specified in transfer.clj
                :data
                {:select-rows-from
                 [
                  ;; !!! Woa any key-value pair I stuff in pops up in the db.sql-select-rows-from fn.
                  {:dbase "employees" :table "employees"   :rows-displayed 1 :idx (oc/kw-table 0)}
                  {:dbase "employees" :table "departments" :rows-displayed 2 :idx (oc/kw-table 1)}
                  {:dbase "employees" :table "salaries"    :rows-displayed 4 :idx (oc/kw-table 2)}
                  ]}
                ;; {:select-rows-from [{:dbase "employees" :table "departments" :rows-displayed 2 :idx :table0}]}

                ;; {:show-tables-from [{:dbase "employees" :idx 0}]}

                ;; TODO this might work as :select-rows-from
                ;; {:show-tables-with-data-from [{:dbase "employees"}]}

                ;; TODO doesn't work: the hash-map app is empty
                ;; {:show-tables-with-data-from
                ;;  [{:dbase (let [dbase (first (get-in app [:dbase0 :name]))]
                ;;             (l/infod src fn-name "app" app)
                ;;             (l/infod src fn-name "owner" owner)
                ;;             (l/infod src fn-name "dbase" dbase)
                ;;             dbase)}]}

                ;; om/transact! propagates changes back to the original atom
                :on-complete (fn [response]
                               (let [er (t/extend-all response)
                                     r (into [] er)]
                                 ;; (l/infod src fn-name "r" r)
                                 (om/transact! app []
                                               (fn [_]
                                                 ;;(l/infod src fn-name "app" response)
                                                 r))))
                }))
  (render-state [_ state]
                ;; [_ {:keys [err-msg]}]
                (om/build render-multi app))
  )

(om/root view app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html
