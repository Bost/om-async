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
  [{:keys [owner default] :as params}]
  (let [ks (oc/full-ks params)
        active (om/get-state owner ks)
        r (if active "active" default)]
    ;; (om/transact! app ks (fn [] (not active)))
    r))

(l/defnd td
  [{:keys [cell idx-row column] :as params}]
  (let [td-val (get-in cell [:val])
        ;; TODO walking over the data from the server doesn't work properly
        p (into params {:ks-data [:data idx-row column] :kw-active [:active]})]
;;     (l/infod src fn-name "ks-data" ks-data)
;;     (l/infod src fn-name "cell" cell)
    (dom/td {:class (get-css p)
              :onClick (fn [e] (oc/activate (into p {:column column :elem-val td-val})))}
            td-val)))

(l/defnd tr
  [{:keys [css row columns] :as params}]
  (let [ column nil ]
    (dom/tr {:class css}
             (map (fn [cell-val col]
                    (td (into params {:cell cell-val :column col})))
                  row
                  (cycle columns)
                  ))))

(l/defnd render-row
  [{:keys [rows row-keywords] :as params}]
  (map (fn [css row idx-row]
         (tr (into params {:css css
                           :row row
                           :idx-row idx-row
                           })))
       (cycle ["" "odd"]) ;; gives the css
       rows               ;; gives the row
       row-keywords))

(l/defnd get-display
  [{:keys [owner idx] :as params}]
;;   (l/infod src fn-name "owner" owner)
;;   (l/infod src fn-name "idx" idx)
  (let [display (om/get-state owner [idx :display])]
    (if (or (nil? display) display)
      #js {}
      #js {:display "none"})))

(defcomponent table
  [{:keys [header rows table-id] :as params} owner]
  (render-state [_ state]
                (dom/table {:id table-id
                            :style (get-display (into params {:idx (keyword table-id)}))}
                           (dom/thead
                            (dom/tr
                             (map #(dom/th (str %)) header)))
                           (dom/tbody
                            (render-row (into params {:rows rows :columns header})))))
  (did-mount [_]
             ;; (table-sorter owner table-id)
             ;; TODO consider using a map defining {:column sort-type} created by the server
             (let [component (TableSorter.)
                   alphaSort goog.ui.TableSorter.alphaSort
                   numericSort goog.ui.TableSorter.numericSort
                   reverseSort (goog.ui.TableSorter.createReverseSort numericSort)]
               (.decorate component (om/get-node owner))
               (.setSortFunction component 0 alphaSort)
               (.setSortFunction component 1 reverseSort)
               )))


;; TODO rename table to extended-table (i.e. table with its table-control buttons)
(defcomponent table-controler
  [{:keys [app rows-displayed tname tdata] :as params} owner]
  (render-state [_ state]
                (let [
                      header (into [] (keys (first tdata)))
                      buttons [{:name "more-rows" :fnc inc :exec-fnc? (fn [_] true)}
                               {:name "less-rows" :fnc dec :exec-fnc? (fn [cnt-rows] (> cnt-rows 0))}]
                      ;; (:idx app) must be used (no @)
                      ;; if binded in a let-statement outside
                      ]
                  (dom/div {:id (str "div-" (name (:idx app)))}
                           tname
                           (dom/button {:onClick (fn [e]
                                                   (oc/toggle-table (into params {:idx (:idx @app)
                                                                                  }))
                                                   )}
                                       "toggle-table")
                           (dom/span {:id (str "span-" (name (:idx app)))}
                            (for [button buttons]
                              (dom/button {:ref "foo"
                                           :onClick (fn [e]
                                                      (oc/displayed-rows
                                                       (into params {:dbase (:dbase @app)
                                                                     :table (:table @app)
                                                                     :rows-displayed (:rows-displayed @app)
                                                                     :idx (:idx @app)
                                                                     :fnc (:fnc button)
                                                                     :exec-fnc? (:exec-fnc? button)
                                                                     })))}
                                          (:name button))))
                           (let [rows (for [d tdata]
                                        (vals d))]
                             (om/build
                              table (into params {:header header
                                                  :rows rows
                                                  :table-id (name (:idx app))
                                                  :owner owner
                                                  })
                             ))))))

(defcomponent render-data
  [{:keys [tdata] :as params} owner]
  (render-state [_ state]
                (let [dbname (get-in tdata [:dbase])
                      tname (get-in tdata [:table])
                      full-tname (str dbname "." tname)
                      data (get-in tdata [:data])
                      rows (into [] (vals data))
                      ks (into [] (keys data))
                      ]
                  ;;(l/infod src fn-name "ks" ks)
                  (om/build table-controler (into params {:tname full-tname
                                                          :tdata rows ;; TODO seem like overwriting fn input - check it!
                                                          :row-keywords ks
                                                          })))))

(defcomponent render-data-vec
  [{:keys [extended-data] :as params} owner]
  (render-state [_ state]
                (dom/div
                 (for [e extended-data]
                   (om/build render-data (into params {:tdata e}))))))

(defcomponent render
  [{:keys [app-full app idx-table] :as params} owner]
  (render-state [_ state]
                (dom/div {:id (str "render-" idx-table)}
                         (om/build render-data-vec
                                   {:app-full app-full :app app :idx-table idx-table :extended-data [app]})
                         )))

(defcomponent render-multi-c
  [app owner]
  (render-state [_ state]
                (let [
                      params {:app app :owner owner}]
                  (if (zero? (count app))        ;; TODO get rid of 'if'
                    (dom/div {:id "fetching"} "Fetching data...")
                    (dom/div {:id "main"}
                             (dom/button {:onClick (fn [e] (oc/deactivate-all params))} "deactivate-all")
                             (for [[idx app-idx] (map-indexed vector app)]
                               (let [params-new (into params {:app-full app
                                                              :app app-idx :idx-table idx})]
;;                                  (println params-new)
;;                                  (render params-new)
                                 (om/build render params-new)
                                 )
                             )
                    )))))

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
;;                 (render-multi {:app app :owner owner}
                (om/build render-multi-c app
                              ))
  )

(om/root view app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html
