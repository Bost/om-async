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
;; [{method :method, url :url, data :data, on-complete :on-complete]}]
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

(defn get-val [m]
  (get-in m [:val]))

(l/defnd onClick [app owner korks idx-table valx css]
  (let [fn-name "onClick"]
    ;; TODO (js* "debugger;") seems to cause LightTable freeze
    (let [
          s (str valx " clicked")
          r (om/transact! app korks (fn [] {:val s :active true}))
          ;; r (om/transact! app {} (fn [] {:color "red"} ))
          ]
      ;; (l/infod src fn-name "r" r)
      ;; we're not allowed to use cursors outside of the render phase as
      ;; this is almost certainly a concurrency bug!
      ;; (edn-xhr
      ;;  {:method :put
      ;;   :url (str "select/id0")
      ;;   :data {:request data}
      ;;   :on-complete
      ;;   (fn [response]
      ;;     ;; (l/info src fn-name (str "Server response: " response))
      ;;     )})
      r)))

(defn render-td [app owner vx idx-table idx-row column css]
  (let [fn-name "render-td"]
    (let [valx (get-val vx)
          korks [:data
                 (keyword (str "row" idx-row))
                 column]]
      (dom/td
       #js {:onClick (fn [e] (onClick app owner korks idx-table valx css))}
       valx))))

(defn render-row [app owner css row idx-table idx-row columns]
  (let [fn-name "render-row"]
    (let [ column nil ]
      (apply dom/tr #js {:className css}
             (map #(render-td app owner %1 idx-table idx-row %2 css)
                  row
                  (cycle columns)
                  )))))

(defn render-indexed-row [app owner idx-table rows row-indexes columns]
  (let [fn-name "render-indexed-row"]
    (map (fn [css row row-index]
           (render-row app owner css row idx-table row-index columns))
         (cycle ["" "odd"]) ;; gives the css
         rows               ;; gives the row
         row-indexes)))

(defn color [app owner]
  (om/transact! app :toggle
                  (fn [] [{:color "red"}]))
  (println "color executed"))

(defn render-table [app owner tname tdata ks idx-table]
  (let [fn-name "render-table"]
    (l/infod src fn-name "ks" ks)
    (let [header (into [] (keys (first tdata)))]
      (l/infod src fn-name "header" header)
      (let [rows (into [] (map #(into [] (vals (nth tdata %)))
                               (range (count tdata))))
            row-indexes (into [] (range (count rows)))]
        (dom/div nil
                 tname
                 (dom/div #js
                          {:id "foo"
                           :style #js {:backgroundColor "blue"}}
                          "text: blue")
                 (dom/button #js {:onClick #(color app owner)} "btn color")
                 (dom/div nil
                          (dom/table nil
                                     (dom/thead nil
                                                (apply dom/tr nil
                                                       (map #(dom/th nil (str %)) header)))
                                     (apply dom/tbody nil
                                            (render-indexed-row app owner idx-table rows row-indexes header)
                                            )))
                 )))))

(defn render-data [app owner table-idx tdata idx-table]
  (let [fn-name "render-data"]
    (let [dbname (get-in tdata [:dbase])
          tname (get-in dbname [:table])
          fq-name (str dbname "." tname)
          data (get-in tdata [:data])
          rows (into [] (vals data))
          ks (into [] (keys data))
          ]
      (l/infod src fn-name "ks" ks)
      (render-table app owner fq-name rows ks idx-table))))

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
                 (if (= 0 cnt-tables)
                   (do
                     (let [msg (str "Fetching data from dbase: " dbase)]
                       (l/info src fn-name msg)
                       msg))
                   (do
                     (let [extended-data [(get-in app korks)]]
                       (render-data-vec app owner extended-data idx-table)))))))))


(defn render-multi [_ app owner]
  (let [fn-name "render-multi"]
    (let [cnt (count app)
          app-vec (into [] (map-indexed vector app))]
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
;;                       {:dbase "employees" :table "employees"   :idx 0}
                      {:dbase "employees" :table "departments" :idx 1}
;;                       {:dbase "employees" :table "salaries"    :idx 2}
                      ]}
                 ;; {:select-rows-from
                 ;;  [{:dbase "employees" :table "departments" :idx 0}]}

                 ;; {:show-tables-from ["employees"]}

                 ;; {:show-tables-with-data-from [dbase]}

                 ;; {:show-tables-with-data-from
                 ;;  [(first (get-in app [:dbase0 :name]))]}

                    ;; om/transact! propagates changes back to the original atom
                    :on-complete #(om/transact! app [] (fn [_] %))
                    })
                  )
      om/IRenderState
      (render-state [_ {:keys [err-msg]}]
                    ;; (l/info src fn-name "render-state")
                    ;; om.core/build     - build single component
                    ;; om.core/build-all - build many components
                    (om/build construct-component app)
                    ))))

;; Rendering loop on a the "dbase0" DOM element
(om/root view ;; fn of 2 args: application state data,
              ;;               backing React component (owner)
         app-state  ;; atom containing application state
         {:target (gdom/getElement "dbase0")}) ;; dbase0 is in index.html

;; eval server.clj, client.cljs, open http://localhost:8080 in browser
;; (defn contact-server [dbase]
;;   (edn-xhr
;;    {:method :put
;;     :url "fetch"
;;     ;; :data {:select-rows-from ["employees" "departments"]}
;;     ;; :data {:select-rows-from ["departments"]}
;;     ;; :data {:show-tables-from ["employees"]}
;;     :data {:show-tables-with-data-from [dbase]}
;;     :on-complete #(om/transact! app dbaseVal0 (fn [_] %))}))

;; (defn color [app owner]
;;   (om/transact! app :toggle
;;                 (fn [] [{:color "red"}]))
;;   (println "color executed"))
