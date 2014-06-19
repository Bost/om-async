(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-async.utils :as utils]
            [om-async.logger :as logger]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def src "client.cljs; ")

(enable-console-print!)

(def dbaseVal0 (utils/dbase-val-kw 0))

(defn add [s]
  (if (nil? s)
    ""
    (str "." s)))

(defn onClick [dbase table column value]
  (fn [e] (logger/info
           (str "onClick: " dbase (add table) (add column) ": " value "; "
                ;; (pr-str e)
                ))))

(defn tr
  "Display table row. dom-cell-elem cound be dom/td or dom/th"
  [dbase table column-vals
   dom-cell-elem row-vals css-class]
;;   (logger/info (str src "tr: column-vals: " column-vals))
  (apply dom/tr #js {:className css-class}
         (map #(dom-cell-elem
                #js {:onClick (onClick dbase table %1 %2)}
                %2)
              column-vals
              row-vals
              )))

(defn create-key-vector [indexes]
  (into [] (map #(utils/column-val-kw %) indexes)))

(defn rows [kw app col-indexes]
  (apply map vector
         (map #(kw (% app))
              (create-key-vector col-indexes))))

(defn table-elem
  [dbase table
   app col-indexes kw dom-table-elem dom-cell-elem alt-row-css-class]
;;   (logger/info (str src "table-elem: (def app " (pr-str app) ")"))
;;   (logger/info (str src "table-elem: (rows :col-name app " col-indexes ")"))
;;   (logger/info (str src "table-elem: (rows " kw " app " col-indexes ")"))
  (apply dom-table-elem nil
         (map #(tr dbase table %1
                   dom-cell-elem %2 %3)
              (cycle (rows :col-name app col-indexes))
              (rows kw        app col-indexes)
              (cycle ["" alt-row-css-class])
              )))

(def ^:private meths
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
      (send url (meths method) (when data (pr-str data))
        #js {"Content-Type" "application/edn"}))))

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

(defn on-edit [id title]
  (edn-xhr
    {:method :put
     :url (str "class/" id "/update")
     :data {:class/title title}
     :on-complete
     (fn [res]
       (logger/info (str src "server response: " res)))}))

;; (on-edit "my-id" "my-title")

(defn create-table-for-columns [dbase db-table data col-indexes]
  ;; (logger/info (str src "create-table-for-columns: data: " data))
  ;; (logger/info (str src "create-table-for-columns: col-indexes: " col-indexes))
  (dom/div nil
           db-table
           (dom/table nil
                      (table-elem dbase db-table
                                  data col-indexes :col-name dom/thead dom/th "")
                      (table-elem dbase db-table
                                  data col-indexes :col-vals dom/tbody dom/td "odd"))))

(defn get-data [kw parent-data]
  ;; (logger/info (str src "get-data: parent-data: " (pr-str parent-data)))
  (let [child-data (remove nil? (map kw parent-data))]
    ;; (logger/info (str src "get-data: kw: " kw))
    ;; (logger/info (str src "get-data: child-data: " (pr-str child-data)))
    (first child-data)))

(defn column-filter? [elem-idx]
  true) ;; true means no element is filtered out

(defn table-filter? [elem-idx]
  true) ;; true means no element is filtered out

(defn create-table [dbase db-table
                    tdata]
  (let [all-cols (into [] (range (count tdata)))
        displayed-cols (into [] (filter column-filter? all-cols))]
    (create-table-for-columns dbase db-table
                              tdata displayed-cols)))

(defn construct-component [app dbase]
  ;; TODO get rid of 'if'
  ;; (logger/info (str src "construct-component: app: " (pr-str app)))
  (apply dom/div nil
         (let [tables (dbaseVal0 app)
               cnt-tables (count tables)]
           ;; (logger/info (str src "component-constructor: tables: " (pr-str tables)))
           ;; (logger/info (str src "cnt-tables: " cnt-tables))
           (if (= 0 cnt-tables)
             (str "Fetching data from dbase: " dbase)
             (let [all-tables (into [] (range cnt-tables))
                   displayed-tables (into [] (filter table-filter? all-tables))]
               (map #(create-table
                      dbase
                      (get-data (utils/table-name-kw %) tables)
                      (get-data (utils/table-val-kw %) tables))
                    displayed-tables))))))

(defn view [app owner]
  (let [dbase "employees"]
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

;; eval this file and open browser with http://localhost:8080/
