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

(defn full-ks [idx-table ks kw-active]
  (let [ks-idx (into [(keyword (str idx-table))] ks)]
    (into ks-idx kw-active)))

(defn onClick [app owner idx-table ks-data kw-active column elem-val]
  (let [fn-name "onClick"]
    ;; TODO (js* "debugger;") seems to cause LightTable freeze
    (let [ks (full-ks idx-table ks-data kw-active)
          active (om/get-state owner ks)]
      ;; (om/transact! app ks (fn [] (not active))) ;; change application state; use with get-in
      (om/set-state! owner ks (not active))  ;; change local component state

      ;; we're not allowed to use cursors outside of the render phase as
      ;; this is almost certainly a concurrency bug!
      (l/infod src fn-name "elem-val" elem-val)
      (l/infod src fn-name "kw-active" kw-active)
      (l/infod src fn-name "ks-data" ks-data)
      (l/infod src fn-name "ks" ks)
;;       (l/infod src fn-name "bef app" @app)
;; ;;       (om/set-state! owner ks "new-val")  ;; change local component state
;;       (om/transact! app ks-data (fn [] {:val (str "## " ks-data " ##")})) ;; change application state; use with get-in
;;       (l/infod src fn-name "aft app" @app)
      (edn-xhr
       {:method :put
        :url (str "select/" column)
        :data {:request elem-val}
        :on-complete (fn [response]
                       (l/info src fn-name (str "Server response: " response))
                       (om/transact! app ks-data
                                     (fn []
                                       {:val (str "# " (:response response) " #")})) ;; change application state; use with get-in
                       (l/infod src fn-name "@app" @app)
          )})
      )))

(defn get-css [app owner idx-table ks-data kw-active default]
  (let [fn-name "get-css"]
    (let [ks (full-ks idx-table ks-data kw-active)
          active (om/get-state owner ks)
          r (if active "active" default)]
      ;; (om/transact! app ks (fn [] (not active)))
      r)))

(defn td [app owner cell idx-table idx-row column css]
  (let [fn-name "td"]
    (let [td-val (get-in cell [:val])
          ;; TODO walking over the data from the server doesn't work properly
          ks-data [:data idx-row column]
          kw-active [:active]
          ]
      (l/infod src fn-name "ks-data" ks-data)
      (l/infod src fn-name "cell" cell)
      (dom/td #js {:className (get-css app owner idx-table ks-data kw-active css)
                   :onClick (fn [e] (onClick app owner idx-table ks-data kw-active column td-val))}
              td-val))))

(defn tr [app owner css row idx-table idx-row columns]
  (let [fn-name "tr"]
    (let [ column nil ]
      (apply dom/tr #js {:className css}
             (map (fn [cell-val col]
                    (td app owner cell-val idx-table idx-row col css))
                  row
                  (cycle columns)
                  )))))

(defn render-row [app owner idx-table rows row-keywords columns]
  (let [fn-name "render-row"]
    (map (fn [css row idx-row]
           (tr app owner css row idx-table idx-row columns))
         (cycle ["" "odd"]) ;; gives the css
         rows               ;; gives the row
         row-keywords)))

(defn table [app owner tname tdata row-keywords idx-table]
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
                                          (render-row app owner idx-table
                                                      rows row-keywords header))))))))

(defn render-data [app owner table-idx tdata idx-table]
  (let [fn-name "render-data"]
    (let [dbname (get-in tdata [:dbase])
          tname (get-in dbname [:table])
          full-tname (str dbname "." tname)
          data (get-in tdata [:data])
          rows (into [] (vals data))
          ks (into [] (keys data))
          ]
      ;; (l/infod src fn-name "ks" ks)
      (table app owner full-tname rows ks idx-table))))

(defn render-data-vec [app owner extended-data idx-table]
  (let [fn-name "render-data-vec"]
    (let [id (into [] (map-indexed vector extended-data))
          rd (map #(render-data app owner
                                (keyword (first %))
                                (second %)
                                idx-table) id)
          r (apply dom/div nil (into [] rd))]
      ;; (l/infod src fn-name "r" r)
      r)))

(defn init [_]
  (let [fn-name "init"]
    ;; (l/infod src fn-name "_" _)
    {}))

(defn render [_ app owner idx-table]
  (let [fn-name "render"]
    (let [
          dbase (get-in app [:dbase])
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
                     (render-data-vec app owner extended-data idx-table))))))))

(defn render-multi [_ app owner]
  (let [fn-name "render-multi"]
    (let [cnt (count app)
          app-vec (into [] (map-indexed vector app))
          table-idx 0]
      ;; (map #(render _ % owner) app)
      (apply dom/div nil
             (map #(render _ (second %) owner (first %))
                  app-vec)))))

;; 3rd param is a map, associate symbol toggle with the value of the
;; :toggle keyword and "put" it in the opts map
(defn construct-component [app owner {:keys [toggle] :as opts}]
  (let [fn-name "construct-component"]
    (reify
      om/IInitState
      (init-state [_] (init _))
      om/IRenderState
      (render-state [_ {:keys [toggle]}] (render-multi _ app owner)))))

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
          :on-complete #(om/transact! app []
                                      (fn [_]
                                        (let [fn-name "on-complete"]
                                          (l/infod src fn-name "app" %)
                                          %)))
          })
        )
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
