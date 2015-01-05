(ns om-async.onclick
  (:require
            [cljs.reader :as reader]
            [goog.events :as events]
;;             [goog.dom :as gdom]
            [om.core :as om :include-macros true]
;;             [om-tools.dom :as tdom :include-macros true]
            [cljs.core.async :as async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.cli-transform :as t]
;;             ;;[clojure.walk :as walk]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType])
  (:require-macros [om-async.logger :as l]))

(def src "onclick.cljs")

(enable-console-print!)

(defn kw-table [idx]
  (keyword (str "table" idx)))

(defn idx-table [kw-table]
  (let [start (count "table")]
    (reader/read-string (subs (name kw-table) start (+ start 1)))))

(def ^:private http-req-methods {:get "GET" :put "PUT" :post "POST" :delete "DELETE"})

;; "XMLHttpRequest: send HTTP/HTTPS async requests to a web server and load the
;; server response data back into the script"
(l/defnd edn-xhr
  [{:keys [method url data on-complete]}]
;;   (l/infod src fn-name "url" url)
  (let [xhr (XhrIo.)]  ;; instantiate a basic class for handling XMLHttpRequests.
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
      (send url (http-req-methods method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

(defn full-ks
  [{:keys [idx-table ks-data kw-active] :as params}]
;;   (println "idx-table" idx-table)
;;   (println "ks-data" ks-data)
;;   (println "kw-active" kw-active)
  (let [ks-idx (into [idx-table] ks-data)]
    (into ks-idx kw-active)))

(defn full-ks-display
  [{:keys [idx-table kw-display] :as params}]
  [(kw-table idx-table) kw-display])

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

(defn toggle-activate
  [app owner column elem-val active]
  (let [kat (korks-all-tables app column)]
    (doseq [[kti ti i] (map vector kat app (range))]  ;; korks-for-table-i
      (doseq [ktirj kti]
        (let [tirj-val (get-in ti (into ktirj [:val]))]
          (if (= tirj-val elem-val)
            (let [ktirj-active (into ktirj [:active])
                  table-ktirj-active (into [(kw-table i)] ktirj-active)]
              ;; change local component state
              (om/set-state! owner table-ktirj-active active)
              )))))))

(l/defnd activate
  [{:keys [app dbase ks-data column elem-val] :as params} owner]
  ;; TODO (js* "debugger;") seems to cause LightTable freeze
  (let [active (om/get-state owner :active)]
    ;; we're not allowed to use cursors outside of the render phase as
    ;; this is almost certainly a concurrency bug!
    ;; (om/transact! app ks (fn [] (not active))) ;; change application state; use with get-in
    ;; (om/set-state! owner ks (not active))  ;; change local component state

    ;; (toggle-activate @app owner (last ks-data) elem-val (not active-ks-params))
    (om/set-state! owner :active (not active))

    (edn-xhr
     {:method :put
      :url (str "select/" column)
      ;; value under :data can't be a just a "value". (TODO see if only hash-map is accepted)
      :data {:request {:dbase dbase :column column :value elem-val}}
      :on-complete (fn [response]
                     ;; change application state; use with get-in
                     (om/transact! app [] (fn [_]
                                            ;; (println "transacting...")
                                            (let [fn-name "activate-:on-complete"]
                                              (l/infod src fn-name "response" response)
                                              ;; TODO server.clj add {:status :ok} to response; onclick.cljs extract {:status :ok} from response
                                              (t/extend-all response))))
                     )
      })))

(defn deactivate-all
  "Deactivate all active React GUI components"
  [app owner]
  (println "TODO implement deactivate-all; use clojure/walk?"))

(defn displayed-elems [elem add-remove-N]
  nil)

;; (into {:a 1 :b 2} {:a 2 :c 3})
(l/defnd toggle-table
  ;; "TODO Should use the displayed-elems fn work a la Display +N / -N tables
  ;;  Hide table component from web page"
  [{:keys [owner idx] :as params}]
  (l/infod src fn-name "params" params)
  (let [korks [idx :display]
        displayed-state (om/get-state owner korks)
        ;; TODO proper initialisation of table displayed state
        ]
    (l/infod src fn-name "displayed-state" displayed-state)
    (let [
        displayed (if (nil? displayed-state) true displayed-state)]
    (l/infod src fn-name "displayed" displayed)
    (om/set-state! owner korks (not displayed)))))

;; "Display +N / -N rows"; TODO better name for idx (values like :table0)
(l/defnd displayed-rows
  [app {:keys [owner dbase table rows-displayed idx fnc exec-fnc?] :as params}]
;;   (println "app" @app)
  (l/infod src fn-name "params" params)
  (if (exec-fnc? rows-displayed)
    (edn-xhr
     {:method :put
      :url "fetch"
      :data {:name :select-rows-from
             :data {:dbase0 {:name dbase
                             :data {
                                    idx {:name table
                                         :data {:rows-displayed (fnc rows-displayed)}}
                                    }}}}

      :on-complete (fn [response]
                     (let [korks [:data idx]
                           new-data (t/extend-all response)
                           new-data-stripped (get-in new-data korks)
                           ]
                       (l/infod src fn-name "response" response)
                       (l/infod src fn-name "new-data" new-data)
                       (l/infod src fn-name "new-data-stripped" new-data-stripped)
                       ;om/transact! propagates changes back to the original atom
                       ;; (om/transact! app korks (fn [_] new-data-stripped))
                       (om/update! app korks new-data-stripped)
                       ))
      })))
