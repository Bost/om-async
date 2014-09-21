(ns om-async.client
;;   (:use [jayq.core :only [$ css html document-ready]])
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [om.dom :as odom :include-macros true]
            ;; this is probably not needed at the moment
            [cljs.core.async :refer [put! chan <!]]
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

(defcomponent table [{:keys [app owner header rows table-id] :as params}]
  (render [_]
          ;; (println "render: table-id: " table-id)
          (dom/table {:id table-id
                      :style (get-display (into params {:idx (:idx app)}))}
                     (dom/thead
                      (dom/tr
                       (map #(dom/th (str %)) header)))
                     (dom/tbody
                      (render-row (into params {:rows rows :columns header})))))
  (did-mount [_]
             ;; (table-sorter owner table-id)
             (let [
                   el (gdom/getElement table-id)
                   ;; el (om/get-node owner)
                   ;; el (om/get-node owner [table-id])
                   ;; el (om/get-node owner table-id)
                   ]
               ;; (println "el: " el)
               ;; (println "el-id: " (:id el))

               ;; TODO consider using a map defining {:column sort-type} created by the server
               (if (nil? el)
                 (println (str "ERROR: (gdom/getElement " table-id ") is nil: " el))
                 (let [component (TableSorter.)
                       alphaSort goog.ui.TableSorter.alphaSort
                       numericSort goog.ui.TableSorter.numericSort
                       reverseSort (goog.ui.TableSorter.createReverseSort numericSort)]
                   (.decorate component el)
                   (.setSortFunction component 0 alphaSort)
                   (.setSortFunction component 1 reverseSort)
                   ))
               )
             )
  )

;; TODO rename table to extended-table (i.e. table with its table-control buttons)
(l/defnd table-controler
  [{:keys [app owner idx rows-displayed tname tdata] :as params}]
  (let [rows (into [] (map #(into [] (vals (nth tdata %)))
                           (range (count tdata))))
        header (into [] (keys (first tdata)))
        buttons [{:name "more-rows" :fnc inc :exec-fnc? (fn [_] true)}
                 {:name "less-rows" :fnc dec :exec-fnc? (fn [cnt-rows] (> cnt-rows 0))}]
        ;; (:idx app) must be used (no @)
        ;; if binded in a let-statement outside
        ]
    ;; (l/infod src fn-name "params" params)
    ;; (l/infod src fn-name "owner" owner)
    ;; (l/infod src fn-name "rows-displayed" (:rows-displayed app))
    ;; (l/infod src fn-name "table: idx" (:idx app))
    (dom/div {:id idx}
             tname
             (dom/button {:onClick (fn [e]
                                     (oc/toggle-table (into params {:idx (:idx @app)}))
                                     )}
                         "toggle-table")
             (dom/span
              (map
               #(dom/button {:ref "foo"
                             :onClick (fn [e]
                                        (oc/displayed-rows
                                         (into params {:dbase (:dbase @app)
                                                       :table (:table @app)
                                                       :rows-displayed (:rows-displayed @app)
                                                       :idx (:idx @app)
                                                       :fnc (:fnc %)
                                                       :exec-fnc? (:exec-fnc? %)
                                                       })))}
                            (:name %)) buttons))
             (let [table-id (name (:idx app))]
               (om/build
                table (into params {:header header :rows rows :table-id table-id}))
               ))))

(l/defnd render-data
  [{:keys [tdata] :as params}]
  (let [dbname (get-in tdata [:dbase])
        tname (get-in tdata [:table])
        full-tname (str dbname "." tname)
        data (get-in tdata [:data])
        rows (into [] (vals data))
        ks (into [] (keys data))
        ]
    ;;(l/infod src fn-name "ks" ks)
    (table-controler (into params {:tname full-tname
                         :tdata rows ;; TODO seem like overwriting fn input - check it!
                         :row-keywords ks}))))

(l/defnd render-data-vec
  [{:keys [extended-data] :as params}]
  (let [id (into [] (map-indexed vector extended-data)) ;; TODO check what exactly do I need here
        rd (map #(render-data (into params {:tdata (second %)})) id)
        r (dom/div (into [] rd))]
    r))

(l/defnd render
  [{:keys [app idx] :as params}]
  ;; (l/infod src fn-name "params" params)
  (dom/div {:id idx}
           (render-data-vec (into params {:extended-data [app]}))))

(l/defnd render-multi
  [{:keys [app owner] :as params}]
  ;; (l/infod src fn-name "app" app)
  (if (zero? (count app))        ;; TODO get rid of 'if'
    (dom/div {:id "fetching"} "Fetching data...")
    (dom/div {:id "main"}
     (dom/button {:onClick (fn [e] (oc/deactivate-all params))} "deactivate-all")
     (map #(render (into params {
                                 :app-full app
                                 ;; the original value under :app is [{..}]; new value is just the {...}
                                 :app (second %)
                                 :idx-table (first %)}))
          (into [] (map-indexed vector app))))))

(defcomponent view
  ;; "data - application state data (a cursor); owner - backing React component
  ;; returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
  ;; IInitState
  ;; IWillMount
  ;; IDidMount
  ;; IShouldUpdate
  ;; IWillReceiveProps
  ;; IWillUpdate
  ;; IDidUpdate
  ;; IRender
  ;; IRenderState
  ;; IDisplayName
  ;; IWillUnmount

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
                (render-multi {:app app :owner owner}
                              ))
  )

;; Rendering loop on a the "dbase0" DOM element
(om/root view ;; fn of 2 args: application state data,
              ;;               backing React component (owner)
         app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html
