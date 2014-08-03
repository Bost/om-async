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

(def app-state
  (atom {
         :x ["def-state"]
;;          :dbase0 {}
;;          :dbase0 {:toggle {:in "in-val"} :name ["employees"] :vals []} ;; TODO create dbase0 by transfer.clj
;;          :toggle #{nil}
         }))

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

(defn onClick [v owner idx row col]
  (let [fn-name "onClick"]
;;     (l/infod src fn-name "v" v)
;;     (l/infod src fn-name "owner" owner)
;;     (l/infod src fn-name "idx" idx)
;;     (l/infod src fn-name "row" row)
;;     (l/infod src fn-name "col" col)
    (fn [e]
      (l/infod src fn-name "v" v)
      (let [korks
;;             [:x :data row col :active]
            [:x]
            as @app-state
            ]
        (l/infod src fn-name "korks" korks)
        (l/infod src fn-name "as" as)
        (l/infod src fn-name "(type owner)" (type owner))
        (let [
              ;; ev (get-in as korks)
              ev (om/get-state owner korks)
              ]
          (l/infod src fn-name "ev" ev)
;;           (let [
;;                 e1 (om/get-state owner korks)
;;                 e2 (om/set-state! owner korks true)
;; ;;                 toggled-elems-2 (om/transact! data :toggle (fn [_] true))
;;                 e3 (om/get-state owner korks)
;; ;;                 isIn (u/contains-value? toggled-elems data)
;;                 ]

;;             (l/infod src fn-name "e1" e1)
;; ;;             (l/infod src fn-name "e2" e2)
;;             (l/infod src fn-name "e3" e3)
;;             )


          v)))))

(defn onClick-orig [owner dbase table col row-value]
  (let [fn-name "onClick"]
    (fn [e]
      (let [idx 0
            kw-dbase (u/kw-prefix :dbase idx)
            kw-table (u/kw-prefix :table idx)
            kw-col (u/kw-prefix :col idx)
            kw-row-value (u/kw-prefix :row idx)
            data {kw-dbase dbase
                  kw-table table
                  kw-col col
                  kw-row-value row-value}]
        (l/infod src fn-name "data" data)  ;; impossible to work with data

        (l/info src fn-name (str "pr-str owner: " (pr-str owner)))
        (l/infod src fn-name "owner" owner)
        (l/infod src fn-name "kw-dbase" kw-dbase)
        (l/infod src fn-name "dbase" dbase)
        (l/infod src fn-name "kw-table" kw-table)
        (l/infod src fn-name "kw-col" kw-col)
        (l/infod src fn-name "kw-row-value" kw-row-value)
        (let [korks
;;               [kw-dbase :vals kw-table :vals kw-col :vals]
              [kw-dbase :toggle :in]
;;               [kw-dbase :vals]
;;               [kw-dbase]
              ]
          (l/infod src fn-name "korks" korks)
          (let [
                toggled-elems-1 (om/get-state owner korks)
                toggled-elems-2 (om/set-state! owner korks true)
;;                 toggled-elems-2 (om/transact! data :toggle (fn [_] true))
                toggled-elems-3 (om/get-state owner korks)
;;                 isIn (u/contains-value? toggled-elems data)
                ]

            (l/infod src fn-name "toggled-elems-1" toggled-elems-1)
;;             (l/infod src fn-name "toggled-elems-2" toggled-elems-2)
            (l/infod src fn-name "toggled-elems-3" toggled-elems-3)
;;             (l/infod src fn-name "isIn" isIn)
            ;; (if (isIn)
            ;;   ;; TODO serch if om has some 'state-remove' function
            ;;   (om/set-state! owner :toggle data))
            )
          )

;;         (edn-xhr
;;          {:method :put
;;           :url (str "select/id0")
;;           :data {:request data}
;;           :on-complete
;;           (fn [response]
;;             ;; (l/info src fn-name (str "Server response: " response))
;;             )})
        ))))

(defn column-filter? [elem-idx] true) ;; no element is filtered out
;; (let [m {:a 1 :b 2 :c 1}]
;;   (select-keys m (for [[k v] m :when (= v 1)] k)))

(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out
;; (defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no element is filtered out

(defn get-val [m]
  (get-in m [:val]))

(defn render-row [owner css row]
  (let [fn-name "render-row"]
    (l/infod src fn-name "css" css)
    (l/infod src fn-name "row" row)
    (apply dom/tr #js {:className css}
           (map #(dom/td
                  #js {:onClick (onClick (get-val %) owner 1 :row0 :gender)}
                  ;; #js {:onClick (onClick owner dbase table %1 %2)}
                  (get-val %)) row))))

(defn render-table [owner tname tdata]
  (let [fn-name "render-table"]
    (l/infod src fn-name "tname" tname)
    (l/infod src fn-name "tdata" tdata)
    (let [header (into [] (keys (first tdata)))]
      (l/infod src fn-name "header" header)
      (let [rows (into [] (map #(into [] (vals (nth tdata %)))
                               (range (count tdata))))]
        (l/infod src fn-name "rows" rows)
        (dom/div nil
                 tname
                 (dom/div nil
                          (dom/table nil
                                     (dom/thead nil
                                                (apply dom/tr nil
                                                       (map #(dom/th nil (str %)) header)))
                                     (apply dom/tbody nil
                                            (map #(render-row owner %1 %2)
                                                 (cycle ["" "odd"])
                                                 rows)))))))))

(defn render-data [owner data]
  (let [fn-name "render-data"]
    (l/infod src fn-name "data" data)
    (let [dbname (get-in data [:dbase])
          tname (get-in data [:table])
          fq-name (str dbname "." tname)
          tdata (into [] (vals (get-in data [:data])))]
      (l/infod src fn-name "tname" tname)
      (l/infod src fn-name "tdata" tdata)
      (render-table owner fq-name tdata))))

(defn render-data-vec [owner data]
  (let [fn-name "render-data-vec"]
    (l/infod src fn-name "data" data)
    (let [r (into [] (map #(render-data owner %) data))]
      (l/infod src fn-name "r" r)
      (apply dom/div nil r))))

(defn construct-component [data owner {:keys [toggle] :as opts}]
  (let [fn-name "construct-component"]
    (l/infod src fn-name "data" data)
    ;; (l/infod src fn-name "owner" owner)
    (reify
      om/IInitState (init-state [_]
                                (l/infod src fn-name "_" _)
;;                                 {:dbase0 {:name ["employees"] :vals ["init-val"]}}
                                {:x ["init-state"]}
                                )
      om/IRenderState
      (render-state [_ {:keys [toggle]}]
                    (let [dbase (first (get-in data [:x :dbase]))]
                      ;; TODO get rid of 'if'
                      (l/infod src fn-name "dbase" dbase)
                      (dom/div nil
                             (let [tables (get-in data [:x])
                                   cnt-tables (count tables)]
                               ;; (l/infod src fn-name "tables" tables)
                               ;; (l/infod src fn-name "cnt-tables" cnt-tables)
                               (if (= 0 cnt-tables)
                                 (let [msg (str "Fetching data from dbase: " dbase)]
                                   (l/info src fn-name msg)
                                   msg)
                                 (let [extended-data
                                       ;; [data]
                                       [(get-in data [:x])]
                                       ]
                                   ;; (l/infod src fn-name "extended-data" extended-data)
                                   (let [r (render-data-vec owner extended-data)
;;                                          s0 (om/get-state owner [:x])
;;                                          s1 (om/set-state! owner [:x] {:a 1})
                                         s1 (om/set-state! owner data)
                                         s2 (om/get-state owner [:x])
                                         ]
                                     (l/infod src fn-name "(type data)" (type data))
                                     (l/infod src fn-name "(type owner)" (type owner))
;;                                      (l/infod src fn-name "s0" s0)
                                     (l/infod src fn-name "s1" s1)
                                     (l/infod src fn-name "s2" s2)
                                     r)
                                   )))))))))

(defn view [data owner]
  (let [fn-name "view"]
    (l/infod src fn-name "data" data)
;;     (l/infod src fn-name "owner" owner)
    (reify
      om/IWillMount
      (will-mount [_]
                  (l/info src fn-name "will-mount")
                  (edn-xhr
                   {:method :put
                    :url "fetch"
                    ;; TODO the idx should be specified in transfer.clj
                    :data {:select-rows-from [
                                              {:dbase "employees" :table "departments" :idx 0}
                                              {:dbase "employees" :table "employees"   :idx 1}
;;                                               {:dbase "employees" :table "salaries"    :idx 2}
                                              ]}
;;                     :data {:select-rows-from [{:dbase "employees" :table "departments" :idx 0}]}
;;                     :data {:show-tables-from ["employees"]}
                    ;; :data {:show-tables-with-data-from [dbase]}
;;                     :data {:show-tables-with-data-from [(first (get-in data [:dbase0 :name]))]}
                    :on-complete #(om/transact! data [:x] (fn [_] %))})
                  )
      om/IRenderState
      (render-state [_ {:keys [err-msg]}]
                    (l/info src fn-name "render-state")
                    (om/build construct-component data)
                    ))))

(om/root view app-state {:target (gdom/getElement
                                  "dbase0")}) ;; dbase0 is in index.html

;; eval server.clj, client.cljs, open browser with http://localhost:8080
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
