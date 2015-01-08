(ns om-async.client
;;   (:use [jayq.core :only [$ css html document-ready]])
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            ;; [om.dom :as orig-dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as otdom :include-macros true]
            ;; this is probably not needed at the moment
            [cljs.core.async :as async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.onclick :as oc]
            [om-async.cli-transform :as t]
            [omdev.core :as omdev] ; data inspection & history component
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
  [{:keys [app dbase cell idx-table idx-row column] :as params} owner]
  (render-state [_ state]
                (let [fn-name "defcomponent-td"]
                  (let [td-val (get-in cell [:val])
                        ;; TODO walking over the data from the server doesn't work properly
                        p (into params {:ks-data [:data idx-row column]
                                        :kw-active [:active]
                                        :dbase dbase
                                        :idx-table idx-table
                                        :idx-row idx-row
                                        :column column
                                        :elem-val td-val
                                        })]
                    (l/infod src fn-name "p" p)
                    (otdom/td {:id (str idx-table "-" idx-row "-" column "-" td-val)
                             :class (if (om/get-state owner :active) "active" nil)
                             :onClick (fn [e] (oc/activate p owner))}
                            td-val)))))

(defcomponent tr
  [{:keys [app dbase css row columns idx-table idx-row] :as params} owner]
  (render-state [_ state]
                (let [ column nil ]
                  ;;     (println "row: " row)
                  ;;     (println "columns: " columns)
                  (otdom/tr {:class css}
                          (for [col-kw (keys row)]
                            ;; (println "col-kw: " col-kw)
                            (om/build td {:app app
                                          :dbase dbase
                                          :cell (col-kw row)
                                          :column col-kw
                                          :idx-table idx-table
                                          :idx-row idx-row
                                          }))))))

(defcomponent render-rows
  [{:keys [app dbase rows idx-table] :as params} owner]
  ;;   (println "rows: " rows)
  ;;   (println "(keys rows): " (keys rows))
  ;;   (println "idx-table" idx-table)
  (render-state [_ state]
                (otdom/tbody
                 (for [row-kw (keys rows)]
                   ;;     (println "row: " row)
                   ;;     (println "idx-row: " idx-row)
                   (om/build tr {:app app
                                 :dbase dbase
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
  [{:keys [app dbase header rows table-id idx-table] :as params} owner]
  (render-state [_ state]
                ;;                 (println "header: " header)
                ;;                 (println "table-id: " table-id)
                ;;                 (println "rows: " rows)
                ;;                 (println "idx-table" idx-table)
                (otdom/table {:id table-id
                            :style (get-display (keyword table-id) owner)}
                           (otdom/thead
                            (otdom/tr
                             (for [h header]
                               (otdom/th (str h)))))
                           (om/build render-rows {:app app
                                                  :dbase dbase
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

;; TODO DRY!: displayed? in client.cljs and onclick.cljs are the same
(l/defnd displayed? [owner korks]
  (let [displayed-state (om/get-state owner korks)
        displayed (if (nil? displayed-state) true displayed-state)]
    displayed))

;; TODO rename table to extended-table (i.e. table with its table-control buttons)
(defcomponent table-controller [app owner]
  (render-state [_ state]
                (let [fn-name "defcomponent-table-controller"]
                  (otdom/div
                   (let [dbname (get-in app [:dbase])
                         dbdata (get-in app [:data])
                         ]
                     (for [table-key (keys dbdata)]
                       (let [table (table-key dbdata)
                             tname (get-in table [:table])
                             full-tname (str dbname "." tname)
                             data (get-in table [:data])
                             rows-displayed (count data)
                             displayed (displayed? owner [(:idx table) :display])

                             header (into [] (keys (:row0 data)))
                             buttons [
                                      ;; max 5 rows displayed per table on the client
                                      {:name "more-rows" :fnc inc :exec-fnc? (fn [cnt-rows] (< cnt-rows 5))}
                                      {:name "less-rows" :fnc dec :exec-fnc? (fn [cnt-rows] (> cnt-rows 0))}]
                             ]
                         (otdom/div {:id (str "div-" (name (:idx table)))}
                                  (str tname "-" (name (:idx table)))
                                  (otdom/button {:onClick (fn [e]
                                                          (oc/toggle-table {:owner owner :idx (:idx @table)}))}
                                              "toggle-table")
                                  (otdom/span {:id (str "span-" (name (:idx table)))}
                                            (for [button buttons]
                                              (otdom/button {:ref "foo"
                                                           :onClick (fn [e]
                                                                      (oc/displayed-rows app
                                                                                         {:owner owner
                                                                                          :dbase dbname
                                                                                          :table (:table @table)
                                                                                          :rows-displayed rows-displayed
                                                                                          :idx (:idx @table)
                                                                                          :fnc (:fnc button)
                                                                                          :exec-fnc? (:exec-fnc? button)
                                                                                          }))}
                                                          (:name button)))
                                            (str "displayed / all: " rows-displayed "/" (:row-count table)))

                                  (if displayed
                                    (om/build table-c {:app app
                                                       :dbase dbname
                                                       :header header
                                                       :rows data
                                                       :table-id (name (:idx table))
                                                       :idx-table (:idx table)
                                                       }))
                                  ))))))))

(defcomponent render-multi [app owner]
  (render-state [_ state]
                (if (zero? (count app))        ;; TODO get rid of 'if'
                  (otdom/div {:id "fetching"} "Fetching data...")
                  (otdom/div {:id "main"}
                           (otdom/button {:onClick (fn [e] (oc/deactivate-all app owner))} "deactivate-all")
                           (om/build table-controller app)))))

(defcomponent view
  ;; "data - application state data (a cursor); owner - backing React component
  ;; returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
  (will-mount [_]
              (oc/edn-xhr
               {:method :put
                :url "fetch"
                :data
                {:name :select-rows-from
                 :data {:dbase0 {:name "employees"
                                 :data {
                                        :table0 {:name "employees"
                                                 :data {:rows-displayed 2}}
;;                                         :table1 {:name "departments"
;;                                                  :data {:rows-displayed 2}}
;;                                         :table2 {:name "salaries"
;;                                                  :data {:rows-displayed 2}}
                                        }}}}

;;                 {:name :show-tables-from
;;                  :data {:dbase0 {:name "employees"
;;                                  :data {
;;                                         :table0 {:name "all-tables"
;;                                                  :data {:rows-displayed 3}}}}}}

;;                 {:name :show-tables-with-data-from
;;                  :data {:dbase0 {:name "employees"
;;                                  :data {
;;                                         :table0 {:name "all-tables-with-data-from"
;;                                                  :data {:rows-displayed 2}}}}}}

                ;; om/transact! propagates changes back to the original atom
                :on-complete (fn [response]
                               (om/transact! app [] (fn [_]
                                                      ;; (println "transacting...")
                                                      (let [fn-name "view-:on-complete"]
                                                        (l/infod src fn-name "response" response)
                                                        (t/extend-all response)))))
                }))
  (render-state [_ state]
                ;; (println "rendering...")
                ;; [_ {:keys [err-msg]}]
                (om/build render-multi app)))

;; (om/root view app-state  ;; atom containing application state
;;          {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html

;; om/root replaced with omdev/dev-component
(omdev/dev-component view app-state
    {:target (.getElementById js/document "dbase0")
     :tx-listen (fn [tx-data root-cursor]
                  ;;(println "listener 1: " tx-data)
                  )})
(println "-------------------------------------------------")
