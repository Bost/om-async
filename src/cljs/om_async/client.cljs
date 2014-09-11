(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.cli-transform :as t]
            ;;[clojure.walk :as walk]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType])
  (:require-macros [om-async.logger :as l]))

(def src "client.cljs")

(enable-console-print!)

(def ^:private http-req-methods {:get "GET" :put "PUT" :post "POST" :delete "DELETE"})

;; prefix for table index
(def table-prefix "table")

;; The 'client dbase'. swap! or reset! on app-state trigger root re-rendering
(def app-state (atom {}))

;; [{:keys [dbase table]}] is a special form for
;; [{method :method, url :url, data :data, on-complete :on-complete}]
(defn edn-xhr
  "XMLHttpRequest: send HTTP/HTTPS async requests to a web server and load the
  server response data back into the script"
  [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]  ;; instantiate a basic class for handling XMLHttpRequests.
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
      (send url (http-req-methods method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

;; TODO consider using take-while/drop-while to increase performance
(defn column-filter? [elem-idx] true) ;; no element is filtered out
;; (let [m {:a 1 :b 2 :c 1}]
;;   (select-keys m (for [[k v] m :when (= v 1)] k)))

(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out
;; (defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no element is filtered out

(defn full-ks
  [{:keys [idx-table ks-data kw-active] :as params}]
  (let [ks-idx (into [(keyword (str table-prefix idx-table))] ks-data)]
    (into ks-idx kw-active)))

(l/defnd ks-other [idx-table cell-val]
;;   (l/infod src fn-name "idx-table" idx-table)
;;   (l/infod src fn-name "cell-val" cell-val)
  (if (not (nil? cell-val))
    [(keyword (str table-prefix idx-table)) :data :row1 :emp_no :active]))

(defn nth-korks
  "Returns [[:data elem0 :emp_no] [:data elem1 :emp_no] [:data elem2 :emp_no]]"
  [vec-of-elems column]
  (into []
        (map #(into [:data] [% column]) vec-of-elems)))

(defn all-idx-table-kws
  "Returns [[:row0 :row1] [:row0 :row1] [:row0 :row1]]"
  [app]
  (into []
        (for [i (map :data app)]
          (into [] (keys i)))))

(defn korks-all-tables
  "Returns [
    [[:data :row0 :emp_no] [:data :row1 :emp_no]]  ;; for :table0
    [[:data :row0 :emp_no] [:data :row1 :emp_no]]  ;; for :table1
    [[:data :row0 :emp_no] [:data :row1 :emp_no]]  ;; for :table2
  ]"
  [app column]
  (into [] (map #(nth-korks % column) (all-idx-table-kws app))))

(defn toggle-cells
  [app owner column elem-val active]
  (let [kat (korks-all-tables app column)]
    (doseq [[kti ti i] (map vector kat app (range))]  ;; korks-for-table-i
      (doseq [ktirj kti]
        (let [tirj-val (get-in ti (into ktirj [:val]))]
          (if (= tirj-val elem-val)
            (let [ktirj-active (into ktirj [:active])
                  table-ktirj-active (into [(keyword (str table-prefix i))] ktirj-active)]
              ;; change local component state
              (om/set-state! owner table-ktirj-active active))))))))

(l/defnd onClick
  [{:keys [app-full app owner ks-data column elem-val] :as params}]
  ;; TODO (js* "debugger;") seems to cause LightTable freeze
  (let [ks (full-ks params)
        active (om/get-state owner ks)]
    ;; we're not allowed to use cursors outside of the render phase as
    ;; this is almost certainly a concurrency bug!
    ;; (om/transact! app ks (fn [] (not active))) ;; change application state; use with get-in
    ;; (om/set-state! owner ks (not active))  ;; change local component state

    ;; (l/infod src fn-name "elem-val" elem-val)
    ;; (l/infod src fn-name "ks" ks)
    ;; (l/infod src fn-name "ks-data" ks-data)
    ;; (l/infod src fn-name "app" @app)
    ;; (l/infod src fn-name "owner" owner)
    (toggle-cells @app-full owner (last ks-data) elem-val (not active))

    (edn-xhr
     {:method :put
      :url (str "select/" column)
      ;; value under :data can't be a just a "value". (TODO see if only hash-map is accepted)
      :data {:request elem-val}
      :on-complete (fn [response]
                     (let [fn-name "onClick-onComplete"]
                       (l/info src fn-name (str "Server response: " response))
                       ;; change application state; use with get-in
                       ;; (om/transact! app ks-data
                       ;;               (fn []
                       ;;                 {:val (str "# " (:response response) " #")}))
                       ))
      })))

(l/defnd get-css
  [{:keys [owner default] :as params}]
  (let [ks (full-ks params)
        active (om/get-state owner ks)
        r (if active "active" default)]
    ;; (om/transact! app ks (fn [] (not active)))
    r))

(l/defnd td
  [{:keys [cell idx-row column] :as params}]
  (let [td-val (get-in cell [:val])
        ;; TODO walking over the data from the server doesn't work properly
        ks-data [:data idx-row column]
        kw-active [:active]
        p (into params {:ks-data ks-data :kw-active kw-active})
        ]
;;     (l/infod src fn-name "ks-data" ks-data)
;;     (l/infod src fn-name "cell" cell)
    (dom/td #js {:className (get-css p)
                 :onClick (fn [e] (onClick (into p {:column column :elem-val td-val})))}
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

(l/defnd table
  [{:keys [tname tdata] :as params}]
  ;; (l/infod src fn-name "row-keywords" row-keywords)
  (let [rows (into [] (map #(into [] (vals (nth tdata %)))
                           (range (count tdata))))
        header (into [] (keys (first tdata)))]
    ;; (l/infod src fn-name "header" header)
    ;; (l/infod src fn-name "rows" rows)
    (dom/div nil
             tname
             (dom/div nil
                      (dom/table nil
                                 (dom/thead nil
                                            (apply dom/tr nil
                                                   (map #(dom/th nil (str %)) header)))
                                 (apply dom/tbody nil
                                        (render-row (into params {:rows rows :columns header})
                                                    )))))))

(l/defnd render-data
  [{:keys [tdata] :as params}]
  (let [dbname (get-in tdata [:dbase])
        tname (get-in dbname [:table])
        full-tname (str dbname "." tname)
        data (get-in tdata [:data])
        rows (into [] (vals data))
        ks (into [] (keys data))
        ]
    ;; (l/infod src fn-name "ks" ks)
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
  [{:keys [app] :as params}]
  (l/infod src fn-name "app" app)
  (let [dbase (get-in app [:dbase])
        korks []]
    ;; TODO get rid of 'if'
    (dom/div nil
             (let [tables (get-in app korks)
                   cnt-tables (count tables)]
               (if (zero? cnt-tables)
                 (let [msg (str "Fetching data from dbase: " dbase)]
                   (l/info src fn-name msg)
                   msg)
                 (let [extended-data [tables]]
                   (render-data-vec
                    (into params {:extended-data extended-data}))))))))

(l/defnd render-multi
  [{:keys [app] :as params}]
  (let [cnt (count app)
        app-vec (into [] (map-indexed vector app))]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "app-vec" app-vec)
    (apply dom/div nil
           (map #(render (into params {
                                       :app-full app
                                       ;; the original value under :app is [{..}]; new value is just the {...}
                                       :app (second %)
                                       :idx-table (first %)}))
                app-vec))))

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
        (edn-xhr
         {:method :put
          :url "fetch"
          ;; TODO the idx should be specified in transfer.clj
          :data
          {:select-rows-from
           [
            {:dbase "employees" :table "employees"   :idx (keyword (str table-prefix 0))}
            {:dbase "employees" :table "departments" :idx (keyword (str table-prefix 1))}
            {:dbase "employees" :table "salaries"    :idx (keyword (str table-prefix 2))}
            ]}
;;           {:select-rows-from [{:dbase "employees" :table "departments" :idx :table0}]}

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
