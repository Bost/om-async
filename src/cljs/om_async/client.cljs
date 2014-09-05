(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            ;;[clojure.walk :as walk]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType])
  (:require-macros [om-async.logger :as l]))

(def src "client.cljs")

(enable-console-print!)

(def ^:private http-req-methods {:get "GET" :put "PUT" :post "POST" :delete "DELETE"})

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
  (let [ks-idx (into [(keyword (str idx-table))] ks-data)]
    (into ks-idx kw-active)))

(defn onClick
  [{:keys [app owner idx-table ks-data kw-active column elem-val] :as params}]
  (let [fn-name "onClick"]
    ;; TODO (js* "debugger;") seems to cause LightTable freeze
    (let [ks (full-ks params)
          active (om/get-state owner ks)]
      ;; (om/transact! app ks (fn [] (not active))) ;; change application state; use with get-in
      (om/set-state! owner ks (not active))  ;; change local component state

      ;; we're not allowed to use cursors outside of the render phase as
      ;; this is almost certainly a concurrency bug!

      ;; (l/infod src fn-name "elem-val" elem-val)
      ;; (l/infod src fn-name "kw-active" kw-active)
      ;; (l/infod src fn-name "ks-data" ks-data)
      ;; (l/infod src fn-name "ks" ks)
      (edn-xhr
       {:method :put
        :url (str "select/" column)
        ;; value under :data can't be a just a "value". (TODO see if only hash-map is accepted)
        :data {:request elem-val}
        :on-complete (fn [response]
                       (let [fn-name "onClick-onComplete"]
                         (l/info src fn-name (str "Server response: " response))
                         ;; change application state; use with get-in
                         (om/transact! app ks-data
                                       (fn []
                                         {:val (str "# " (:response response) " #")}))))
        })
      )))

(defn get-css
  [{:keys [app owner idx-table ks-data kw-active default] :as params}]
  (let [fn-name "get-css"]
    (let [ks (full-ks params)
          active (om/get-state owner ks)
          r (if active "active" default)]
      ;; (om/transact! app ks (fn [] (not active)))
      r)))

(defn td
  [{:keys [cell idx-table idx-row column css] :as params}]
  (let [fn-name "td"]
    (let [td-val (get-in cell [:val])
          ;; TODO walking over the data from the server doesn't work properly
          ks-data [:data idx-row column]
          kw-active [:active]
          p (into params {:ks-data ks-data :kw-active kw-active :css css})
          ]
      (l/infod src fn-name "ks-data" ks-data)
      (l/infod src fn-name "cell" cell)

      (dom/td #js {:className (get-css p)
                   :onClick (fn [e] (onClick (into p {:column column :elem-val td-val})))}
              td-val))))

(defn tr
  [{:keys [css row idx-table idx-row columns] :as params}]
  (let [fn-name "tr"]
    (let [ column nil ]
      (apply dom/tr #js {:className css}
             (map (fn [cell-val col]
                    (td (into params {:cell cell-val :column col})))
                  row
                  (cycle columns)
                  )))))

(defn render-row
  [{:keys [idx-table rows row-keywords columns] :as params}]
  (let [fn-name "render-row"]
    (map (fn [css row idx-row]
           (tr (into params {:css css
                             :row row
                             :idx-row idx-row})))
         (cycle ["" "odd"]) ;; gives the css
         rows               ;; gives the row
         row-keywords)))

(defn table
  [{:keys [tname tdata row-keywords idx-table] :as params}]
  (let [fn-name "table"]
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
                                          (render-row
                                           (into params
                                                 {:rows rows :row-keywords row-keywords :columns header
                                                  })
                                           ))))))))

(defn render-data
  [{:keys [table-idx tdata idx-table] :as params}]
  (let [fn-name "render-data"]
    (let [dbname (get-in tdata [:dbase])
          tname (get-in dbname [:table])
          full-tname (str dbname "." tname)
          data (get-in tdata [:data])
          rows (into [] (vals data))
          ks (into [] (keys data))
          ]
      ;; (l/infod src fn-name "ks" ks)
      (table (into params {:tname full-tname
                           :tdata rows
                           :row-keywords ks})))))

(defn render-data-vec
  [{:keys [extended-data idx-table] :as params}]
  (let [fn-name "render-data-vec"]
    (let [id (into [] (map-indexed vector extended-data))
          rd (map #(render-data
                    (into params
                          {:table-idx (keyword (first %))
                           :tdata (second %)})
                    ) id)
          r (apply dom/div nil (into [] rd))]
      ;; (l/infod src fn-name "r" r)
      r)))

(defn init [_]
  (let [fn-name "init"]
    ;; (l/infod src fn-name "_" _)
    {}))

(defn render
  [{:keys [app idx-table] :as params}]
  (let [fn-name "render"]
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
                   (let [extended-data [(get-in app korks)]]
                     (render-data-vec
                      (into params {:extended-data extended-data})))))))))

(defn render-multi
  [{:keys [app] :as params}]
  (let [fn-name "render-multi"]
    (let [cnt (count app)
          app-vec (into [] (map-indexed vector app))
          table-idx 0]
      (l/infod src fn-name "app" app)
      (l/infod src fn-name "app-vec" app-vec)
      (apply dom/div nil
             (map #(render (into params {:app (second %) :idx-table (first %)}))
                  app-vec)))))

;; 3rd param is a map, associate symbol toggle with the value of the
;; :toggle keyword and "put" it in the opts map
(defn construct-component [app owner]
  (let [fn-name "construct-component"]
    (reify
      om/IInitState
      (init-state [_] (init _))
      om/IRenderState
      (render-state [_ {}]
                    (render-multi {:app app :owner owner})))))

(defn view
  "data - application state data (a cursor); owner - backing React component
  returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
  (let [fn-name "view"]
    (reify
      om/IWillMount
      (will-mount [_]
        (l/info src fn-name "will-mount")
        (edn-xhr
         {:method :put
          :url "fetch"
          ;; TODO the idx should be specified in transfer.clj
          :data
          {:select-rows-from
           [
;;             {:dbase "employees" :table "employees"   :idx 0}
            {:dbase "employees" :table "departments" :idx 1}
;;             {:dbase "employees" :table "salaries"    :idx 2}
            ]}
          ;; {:select-rows-from
          ;;  [{:dbase "employees" :table "departments" :idx 0}]}

          ;; {:show-tables-from ["employees"]}

          ;; {:show-tables-with-data-from [dbase]}

          ;; {:show-tables-with-data-from
          ;;  [(first (get-in app [:dbase0 :name]))]}

          ;; om/transact! propagates changes back to the original atom
          :on-complete (fn [response]
                         (let [fn-name "view-onComplete"]
                           (om/transact! app []
                                         (fn [_]
                                           (l/infod src fn-name "app" response)
                                           response))))}))
      om/IRenderState
      (render-state [_ {:keys [err-msg]}]
        ;; (l/info src fn-name "render-state")
        ;; om.core/build     - build single component
        ;; om.core/build-all - build many components
        (om/build construct-component app)))))

;; Rendering loop on a the "dbase0" DOM element
(om/root view ;; fn of 2 args: application state data,
              ;;               backing React component (owner)
         app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html
