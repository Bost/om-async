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

;; (l/defnd f [x y z]
;;   (println "l/defnd f:")
;;   (+ x y z))

;; (f 1 2 3)

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

(defn column-filter? [elem-idx] true) ;; no element is filtered out
;; (let [m {:a 1 :b 2 :c 1}]
;;   (select-keys m (for [[k v] m :when (= v 1)] k)))

(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out
;; (defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no element is filtered out

(defn get-val [m]
  (get-in m [:val]))

(defn onClick [app owner korks]
  (let [fn-name "onClick"]
;;     (l/infod src fn-name "app" app)
;;     (l/infod src fn-name "(type data)" (type data))
;;     (l/infod src fn-name "owner" owner)
;;     (l/infod src fn-name "korks" korks)
    (let [s (str korks)
          r (om/transact! app korks
                  (fn [] {:val s :active true}))]
;;       (l/infod src fn-name "r" r)
  ;; we're not allowed to use cursors outside of the render phase as
  ;; this is almost certainly a concurrency bug!
  ;;         (edn-xhr
  ;;          {:method :put
  ;;           :url (str "select/id0")
  ;;           :data {:request data}
  ;;           :on-complete
  ;;           (fn [response]
  ;;             ;; (l/info src fn-name (str "Server response: " response))
  ;;             )})
      r)))

(defn render-td [app owner vx]
  (let [fn-name "render-td"]
;;     (l/infod src fn-name "app" app)
;;     (l/infod src fn-name "(type data)" (type data))
;;     (l/infod src fn-name "owner" owner)
;;     (l/infod src fn-name "vx" vx)
    (let [
          gvx (get-val vx)
          korks [:data :row0 :emp_no]
          vy (get-in app korks)
          gvy (get-val vy)
          ]

      (dom/td
       #js {:onClick (fn [e] (onClick app owner [:data :row0 :emp_no]))}
       gvx))))

(defn render-row [app owner css row]
  (let [fn-name "render-row"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    ;; (l/infod src fn-name "css" css)
    ;; (l/infod src fn-name "row" row)
    (apply dom/tr #js {:className css}
           (map #(render-td app owner %) row))))

(def rows [[{:val 10001, :active false} {:val "1986-06-25 22:00:00", :active false} {:val "1987-06-25 22:00:00", :active false} {:val 60117, :active false}] [{:val 10001, :active false} {:val "1987-06-25 22:00:00", :active false} {:val "1988-06-24 22:00:00", :active false} {:val 62102, :active false}]])
(defn render-table [app owner tname tdata ks]
  (let [fn-name "render-table"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    ;; (l/infod src fn-name "tname" tname)
    (l/infod src fn-name "tdata" tdata)
    (l/infod src fn-name "ks" ks)
    (let [header (into [] (keys (first tdata)))]
      (l/infod src fn-name "header" header)
      (let [indexed-tdata (map-indexed vector tdata)
;;             nrows (into [] (map #(into [] (vals (nth tdata %)))
;;                                indexed-tdata
;;                                ))
            rows (into [] (map #(into [] (vals (nth tdata %)))
                               (range (count tdata))
                               ))
            ]
        (l/infod src fn-name "rows" rows)
        (dom/div nil
                 tname
                 (dom/div nil
                          (dom/table nil
                                     (dom/thead nil
                                                (apply dom/tr nil
                                                       (map #(dom/th nil (str %)) header)))
                                     (apply dom/tbody nil
                                            (map #(render-row app owner %1 %2)
                                                 (cycle ["" "odd"])
                                                 rows)))))))))

(defn render-data [app owner table-idx tdata]
  (let [fn-name "render-data"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    ;; (l/infod src fn-name "table-idx" table-idx)
    ;; (l/infod src fn-name "tdata" tdata)
    (let [dbname (get-in tdata [:dbase])
          tname (get-in dbname [:table])
          fq-name (str dbname "." tname)
          data (get-in tdata [:data])
          rows (into [] (vals data))
          ks (into [] (keys data))
          ]
      ;; (l/infod src fn-name "tname" tname)
      ;; (l/infod src fn-name "rows" rows)
      (l/infod src fn-name "ks" ks)
      (render-table app owner fq-name rows ks))))

(defn render-data-vec [app owner extended-data]
  (let [fn-name "render-data-vec"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    ;; (l/infod src fn-name "extended-data" extended-data)
    (let [id (into [] (map-indexed vector extended-data))
          rd (map #(render-data app owner
                                (keyword (first %))
                                (second %)) id)
          r (apply dom/div nil (into [] rd))]
      ;; (l/infod src fn-name "r" r)
      r)))

(defn init [_]
  (let [fn-name "init"]
    ;; (l/infod src fn-name "_" _)
    {}))

(defn render [_ app owner]
  (let [fn-name "render"]
    ;; (l/infod src fn-name "_" _)
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    (let [dbase (get-in app [:dbase])
          korks []]
      ;; TODO get rid of 'if'
      ;; (l/infod src fn-name "dbase" dbase)
      (dom/div nil
               (let [tables (get-in app korks)
                     cnt-tables (count tables)]
                 ;; (l/infod src fn-name "tables" tables)
                 ;; (l/infod src fn-name "cnt-tables" cnt-tables)
                 (if (= 0 cnt-tables)
                   (do
                     (let [msg (str "Fetching data from dbase: " dbase)]
                       (l/info src fn-name msg)
                       msg))
                   (do
                     (let [extended-data [(get-in app korks)]]
                       ;; (l/infod src fn-name "extended-data" extended-data)
                       (let [r (render-data-vec app owner extended-data)]
                         ;; (l/infod src fn-name "(type data)" (type data))
                         ;; (l/infod src fn-name "(type owner)" (type owner))
                         r)))))))))

(defn construct-component [app owner {:keys [toggle] :as opts}]
  (let [fn-name "construct-component"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
    (reify
      om/IInitState
      (init-state [_] (init _))
      om/IRenderState
      (render-state [_ {:keys [toggle]}] (render _ app owner)))))

(defn view
  "data - application state data (a cursor); owner - backing React component
  returns an Om-component, i.e. a model of om/IRender interface"
  [app owner]
  (let [fn-name "view"]
    ;; (l/infod src fn-name "app" app)
    ;; (l/infod src fn-name "owner" owner)
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
                     [{:dbase "employees" :table "departments" :idx 0}
                      {:dbase "employees" :table "employees"   :idx 1}
                      {:dbase "employees" :table "salaries"    :idx 2}
                      ]}
                 ;; {:select-rows-from
                 ;;  [{:dbase "employees" :table "departments" :idx 0}]}

                 ;; {:show-tables-from ["employees"]}

                 ;; {:show-tables-with-data-from [dbase]}

                 ;; {:show-tables-with-data-from
                 ;;  [(first (get-in app [:dbase0 :name]))]}

                    ;; om/transact! propagates changes back to the original atom
                    :on-complete #(om/transact! app [] (fn [_] %))})
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
