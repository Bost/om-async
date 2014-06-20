(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-async.utils :as u]
            [om-async.logger :as l]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def src "client.cljs")

(enable-console-print!)

(def dbaseVal0 (u/kw :dbase :val 0))

(def ^:private http-req-methods
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn edn-xhr
  "XMLHttpRequest: send HTTP/HTTPS requests to a web server and load the
  server response data back into the script"
  [{:keys [method url data on-complete]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
      (send url (http-req-methods method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

(defn onClick [dbase table column row-value]
  (fn [e]
    (let [idx 0
          data {(u/kw :dbase :name idx) dbase
                (u/kw :table :name idx) table
                (u/kw :col :name idx) column
                (u/kw :row :val idx) row-value}]
      ;; (l/info (str src "data: " data))
      (edn-xhr
       {:method :put
        :url (str "select/id0")
        :data {:request data}
        :on-complete
        (fn [response]
          (l/info src "onClick" (str "Server response: " response)))}))))

(defn tr
  "Display table row. dom-cell-elem cound be dom/td or dom/th"
  [dbase table column-vals
   dom-cell-elem row-vals css-class]
  ;; (l/info (str src "tr: column-vals: " column-vals))
  (apply dom/tr #js {:className css-class}
         (map #(dom-cell-elem
                #js {:onClick (onClick dbase table %1 %2)}
                %2)
              column-vals
              row-vals
              )))

(defn key-vector [indexes]
  (into [] (map #(u/kw :col nil %) indexes)))

(defn rows [kw app col-indexes]
  (apply map vector
         (map #(kw (% app))
              (key-vector col-indexes))))

(defn table-elem
  [dbase table
   app col-indexes kw dom-table-elem dom-cell-elem alt-row-css-class]
  ;; (l/info (str src "table-elem: (def app " (pr-str app) ")"))
  ;; (l/info (str src "table-elem: (rows :name app " col-indexes ")"))
  ;; (l/info (str src "table-elem: (rows " kw " app " col-indexes ")"))
  (apply dom-table-elem nil
         (map #(tr dbase table %1
                   dom-cell-elem %2 %3)
              ;; TODO do (cycle [nil nil ...]) when processing table header
              (cycle (rows :name app col-indexes))
              (rows kw app col-indexes)
              (cycle ["" alt-row-css-class]))))

(def app-state
  (atom {dbaseVal0 []}))

(defn display [show] ;; TODO get rid of 'if'
  (if show
    #js {}
    #js {:display "none"}))

(defn handle-change [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn end-edit [text owner cb]
  (om/set-state! owner :editing false)
  (cb text))

(defn editable [data owner {:keys [edit-key on-edit] :as opts}]
  (reify
    om/IInitState
    (init-state [_] {:editing false})
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text (get data edit-key)]
        (dom/li nil
          (dom/span #js {:style (display (not editing))} text)
          (dom/input
            #js {:style (display editing)
                 :value text
                 :onChange #(handle-change % data edit-key owner)
                 :onKeyPress #(when (== (.-keyCode %) 13)
                                (end-edit text owner on-edit))
                 :onBlur (fn [e]
                           (when (om/get-state owner :editing)
                             (end-edit text owner on-edit)))})
          (dom/button
            #js {:style (display (not editing))
                 :onClick #(om/set-state! owner :editing true)}
            "Edit"))))))

;; (on-edit "my-id" "my-title")
(defn create-table-for-columns [dbase db-table data col-indexes]
  ;; (l/info (str src "create-table-for-columns: data: " data))
  ;; (l/info (str src "create-table-for-columns: col-indexes: " col-indexes))
  (dom/div nil
           db-table
           (dom/table nil
                      (table-elem dbase db-table
                                  data col-indexes :name dom/thead dom/th "")
                      (table-elem dbase db-table
                                  data col-indexes :vals dom/tbody dom/td "odd"))))

(defn get-data [kw parent-data]
  ;; (l/info src "get-data" (str "parent-data: " (pr-str parent-data)))
  (let [child-data (remove nil? (map kw parent-data))]
    ;; (l/info src "get-data" (str " kw: " kw))
    ;; (l/info src "get-data" (str "child-data: " (pr-str child-data)))
    (first child-data)))

(defn column-filter? [elem-idx]
  true) ;; no element is filtered out

(defn table-filter? [elem-idx]
  true) ;; no element is filtered out

(defn create-table [dbase db-table
                    tdata]
  (let [all-cols (into [] (range (count tdata)))
        displayed-cols (into [] (filter column-filter? all-cols))]
    (create-table-for-columns dbase db-table
                              tdata displayed-cols)))

(defn construct-component [app dbase]
  ;; TODO get rid of 'if'
  ;; (l/info  src "component-constructor" (str "app: " (pr-str app)))
  (apply dom/div nil
         (let [tables (dbaseVal0 app)
               cnt-tables (count tables)]
           ;; (l/info  src "component-constructor" (str "tables: " (pr-str tables)))
           ;; (l/info  src "component-constructor" (str "cnt-tables: " cnt-tables))
           (if (= 0 cnt-tables)
             (let [msg (str "Fetching data from dbase: " dbase)]
               (l/info src "construct-component" msg)
               msg)
             (let [all-tables (into [] (range cnt-tables))
                   displayed-tables (into [] (filter table-filter? all-tables))]
               (map #(create-table
                      dbase
                      (get-data (u/kw :table :name %) tables)
                      (get-data (u/kw :table :val %) tables))
                    displayed-tables))))))

(defn view [app owner]
  (let [dbase (name :employees)] ;; (name :kw) => "kw"
    (reify
      om/IWillMount
      (will-mount [_] (edn-xhr
                       {:method :put
                        :url "fetch"
;;                         :data {:select-rows-from ["employees" "departments"]}
;;                         :data {:select-rows-from ["departments"]}
;;                         :data {:show-tables-from ["employees"]}
                        :data {:show-tables-with-data-from [dbase]}
                        :on-complete #(om/transact! app dbaseVal0 (fn [_] %))}))
      om/IRender
      (render [_] (construct-component app dbase)))))

(om/root view app-state {:target (gdom/getElement
                                  "dbase0")}) ;; dbase0 is in index.html

;; eval server.clj, client.cljs, open browser with http://localhost:8080
