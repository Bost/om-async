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

(defcomponent td
  [{:keys [app cell idx-table idx-row column] :as params} owner]
  (render-state [_ state]
                (let [td-val (get-in cell [:val])
                      ;; TODO walking over the data from the server doesn't work properly
                      p (into params {:ks-data [:data idx-row column]
                                      :kw-active [:active]
                                      :idx-table idx-table
                                      :idx-row idx-row
                                      :column column
                                      :elem-val td-val
                                      })]
                  (dom/td {:id (str idx-table "-" idx-row "-" column "-" td-val)
                           :class (if (om/get-state owner :active) "active" default)
                           :onClick (fn [e] (oc/activate p owner))}
                          td-val))))

(defcomponent tr
  [{:keys [app css row columns idx-table idx-row] :as params} owner]
  (render-state [_ state]
                (let [ column nil ]
                  ;;     (println "row: " row)
                  ;;     (println "columns: " columns)
                  (dom/tr {:class css}
                          (for [col-kw (keys row)]
                            ;; (println "col-kw: " col-kw)
                            (om/build td {:app app
                                          :cell (col-kw row)
                                          :column col-kw
                                          :idx-table idx-table
                                          :idx-row idx-row
                                          }))))))

(defcomponent render-rows
  [{:keys [app rows idx-table] :as params} owner]
  ;;   (println "rows: " rows)
  ;;   (println "(keys rows): " (keys rows))
  ;;   (println "idx-table" idx-table)
  (render-state [_ state]
                (dom/tbody
                 (for [row-kw (keys rows)]
                   ;;     (println "row: " row)
                   ;;     (println "idx-row: " idx-row)
                   (om/build tr {:app app
                                 :css ""
                                 :row (row-kw rows)
                                 :idx-table idx-table
                                 :idx-row row-kw
                                 })))))

(l/defnd get-display
  [idx owner]
;;   (l/infod src fn-name "idx" idx)
  (let [display (om/get-state owner [idx :display])]
    (if (or (nil? display) display)
      #js {}
      #js {:display "none"})))

(defcomponent table-c
  [{:keys [app header rows table-id idx-table] :as params} owner]
  (render-state [_ state]
                ;;                 (println "header: " header)
                ;;                 (println "table-id: " table-id)
                ;;                 (println "rows: " rows)
                ;;                 (println "idx-table" idx-table)
                (dom/table {:id table-id
                            :style (get-display (keyword table-id) owner)}
                           (dom/thead
                            (dom/tr
                             (for [h header]
                               (dom/th (str h)))))
                           (om/build render-rows {:app app
                                                  :rows rows
                                                  :columns header
                                                  :idx-table idx-table
                                                  })))
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
;;                 (println app)
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
                              (str tname "-" (name (:idx table)))
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

                              (om/build table-c {:app app
                                                 :header header
                                                 :rows data
                                                 :table-id (name (:idx table))
                                                 :idx-table (:idx table)
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
