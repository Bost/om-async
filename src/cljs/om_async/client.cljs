(ns om-async.client
  (:require [goog.dom :as gdom]
            [jayq.macros]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            ;; this is probably not needed at the moment
            [cljs.core.async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.onclick :as oc]
            [om-async.cli-transform :as t]
            )
  (:import [goog.net XhrIo]
           [goog.ui TableSorter])
  (:require-macros [om-async.logger :as l]))

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
    (dom/td #js {:className (get-css p)
                 :onClick (fn [e] (oc/activate (into p {:column column :elem-val td-val})))}
            td-val)))

(l/defnd tr
  [{:keys [css row columns] :as params}]
  (let [ column nil ]
    (apply dom/tr #js {:className css}
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

(l/defnd table-sorter [elem-id]
  (jayq.macros/ready
   (let [el (gdom/getElement elem-id)]
     (if (nil? el)
       (println "elem-id: " elem-id "; el: " el)
       (let [component (TableSorter.)
             alphaSort goog.ui.TableSorter.alphaSort
             numericSort goog.ui.TableSorter.numericSort
             reverseSort (goog.ui.TableSorter.createReverseSort numericSort)]
         (.decorate component el)
         (.setSortFunction component 1 alphaSort)
         (.setSortFunction component 2 reverseSort)))
     )
   )
  )

(l/defnd table
  [{:keys [app owner idx rows-displayed tname tdata] :as params}]
  (let [rows (into [] (map #(into [] (vals (nth tdata %)))
                           (range (count tdata))))
        header (into [] (keys (first tdata)))
        buttons [{:name "more-rows" :fnc inc :exec-fnc? (fn [_] true)}
                 {:name "less-rows" :fnc dec :exec-fnc? (fn [cnt-rows] (> cnt-rows 0))}]
        ;; (:idx app) must be used (no @)
        ;; if binded in a let-statement outside
        ]
    ;;     (l/infod src fn-name "params" params)
    ;;     (l/infod src fn-name "owner" owner)
    ;;     (l/infod src fn-name "rows-displayed" (:rows-displayed app))
    ;;     (l/infod src fn-name "table: idx" (:idx app))
    (dom/div #js {:style (get-display (into params {:idx (:idx app)}))}
             tname
             (dom/button #js {:onClick (fn [e]
                                         (oc/hide-table (into params {:idx (:idx @app)}))
                                         )}
                         "hide-table")
             (apply dom/span nil
                    (map
                     #(dom/button #js {:onClick (fn [e]
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
;;                (apply dom/div nil
                      (dom/table #js {:id table-id}
                                 (dom/thead nil
                                            (apply dom/tr nil
                                                   (map #(dom/th nil (str %)) header)))
                                 (apply dom/tbody nil
                                        (render-row (into params {:rows rows :columns header}))))

                      )
             (dom/div nil (name (:idx app)))
             (table-sorter (name (:idx app)))
;;                )
             )
    ))

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
    (table (into params {:tname full-tname
                         :tdata rows ;; TODO seem like overwriting fn input - check it!
                         :row-keywords ks}))))

(l/defnd render-data-vec
  [{:keys [extended-data] :as params}]
  (let [id (into [] (map-indexed vector extended-data)) ;; TODO check what exactly do I need here
        rd (map #(render-data (into params {:tdata (second %)})) id)
        r (apply dom/div nil (into [] rd))]
    r))

(l/defnd init [_]
  ;; (l/infod src fn-name "_" _)
  {})

(l/defnd render
  [{:keys [app idx] :as params}]
  ;; (l/infod src fn-name "params" params)
  (dom/div #js {:id idx}
           (render-data-vec
            (into params {:extended-data [app]}))))

(l/defnd render-multi
  [{:keys [app owner] :as params}]
  ;; (l/infod src fn-name "app" app)
  ;; TODO get rid of 'if'
  (if (zero? (count app))
    (dom/div nil  "Fetching data..." )
    (apply dom/div nil
           (dom/button #js {:onClick (fn [e] (oc/deactivate-all params))} "deactivate-all")
           (table-sorter "sortMe")
           (map #(render (into params {
                                       :app-full app
                                       ;; the original value under :app is [{..}]; new value is just the {...}
                                       :app (second %)
                                       :idx-table (first %)}))
                (into [] (map-indexed vector app)))
           )))

;; 3rd param is a map, associate symbol toggle with the value of the
;; :toggle keyword and "put" it in the opts map
(l/defnd construct-component
  [app owner]
  (reify
    om/IInitState
    (init-state [_] (init _))
    om/IRenderState
    (render-state [_ {}]
                  (render-multi {:app app :owner owner}))))

(l/defnd view
;; "data - application state data (a cursor); owner - backing React component
;; returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
    (reify
      om/IWillMount
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
;;           {:select-rows-from [{:dbase "employees" :table "departments" :rows-displayed 2 :idx :table0}]}

;;           {:show-tables-from [{:dbase "employees" :idx 0}]}

          ;; TODO this might work as :select-rows-from
;;           {:show-tables-with-data-from [{:dbase "employees"}]}

          ;; TODO doesn't work: the hash-map app is empty
;;           {:show-tables-with-data-from
;;            [{:dbase (let [dbase (first (get-in app [:dbase0 :name]))]
;;                       (l/infod src fn-name "app" app)
;;                       (l/infod src fn-name "owner" owner)
;;                       (l/infod src fn-name "dbase" dbase)
;;                       dbase)}]}

          ;; om/transact! propagates changes back to the original atom
          :on-complete (fn [response]
                         (let [er (t/extend-all response)
                               r (into [] er)
                               ]
                           ;; (l/infod src fn-name "r" r)
                           (om/transact! app []
                                         (fn [_]
                                           ;;(l/infod src fn-name "app" response)
                                           r)))
                         )
          }))
      om/IRenderState
      (render-state [_ {:keys [err-msg]}]
        ;; (l/info src fn-name "render-state")
        ;; om.core/build     - build single component
        ;; om.core/build-all - build many components
        (om/build construct-component app))))

;; Rendering loop on a the "dbase0" DOM element
(om/root view ;; fn of 2 args: application state data,
              ;;               backing React component (owner)
         app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html
