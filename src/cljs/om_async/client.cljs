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

(def kw-dbase0
  :app-data
  ;;(utils/dbase-data-keyword 0)
  )

(defn tr [dom-cell-elem rows css-class]
  (apply dom/tr #js {:className css-class}
         (map #(dom-cell-elem nil %) rows)))

(defn create-key-vector [indexes]
  (into [] (map #(utils/column-keyword %) indexes)))

(defn rows [key-name app col-indexes]
  (apply map vector
         (map #(key-name (% app))
              (create-key-vector col-indexes))))

(defn table-elem
  [app col-indexes kw-row-vals dom-table-elem dom-cell-elem alt-row-css-class]
  (apply dom-table-elem nil
         (map #(tr dom-cell-elem %1 %2)
              (rows kw-row-vals app col-indexes)
              (cycle ["" alt-row-css-class]))))

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
  (atom {kw-dbase0 []}))

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
    (init-state [_]
      {:editing false})
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

(defn create-table-for-columns [table-name data col-indexes]
;;   (logger/info (str src "create-table-for-columns: data: " data))
;;   (logger/info (str src "create-table-for-columns: col-indexes: " col-indexes))
  (dom/div nil
           table-name
           (dom/table nil
                      (table-elem data col-indexes :col-name dom/thead dom/th "")
                      (table-elem data col-indexes :col-vals dom/tbody dom/td "odd"))))

(defn get-data [kw parent-data]
  ;; (logger/info (str src "get-data: parent-data: " (pr-str parent-data)))
  (let [child-data (remove nil? (map kw parent-data))]
    ;; (logger/info (str src "get-data: kw: " kw))
    ;; (logger/info (str src "get-data: child-data: " (pr-str child-data)))
    (first child-data)))

(defn create-table [tname tdata]
  (let [cnt-columns (count tdata)]
    (create-table-for-columns
     tname tdata (into [] (range cnt-columns)))))

(defn construct-component [app]
  ;; TODO get rid of 'if'
  ;; (logger/info (str src "construct-component: app: " (pr-str app)))
  (apply dom/div nil
         (let [db-data (kw-dbase0 app)
               cnt-tables (count db-data)]
           ;; (logger/info (str src "component-constructor: db-data: " (pr-str db-data)))
           ;; (logger/info (str src "cnt-tables: " cnt-tables))
           (if (= 0 cnt-tables)
             "Fetching data from the dbase... "
             (map #(create-table
                    (get-data (utils/table-name-keyword %) db-data)
                    (get-data (utils/table-data-keyword %) db-data))
                  (into [] (range cnt-tables)))))))


(defn view [app owner]
  (reify
    om/IWillMount
    (will-mount [_] (edn-xhr
                     {:method :put
                      :url "fetch"
;;                       :data {:select-rows-from ["employees" "departments"]}
;;                       :data {:select-rows-from ["departments"]}
;;                       :data {:show-tables-from ["employees"]}
                      :data {:show-tables-with-data-from ["employees"]}
                      :on-complete #(om/transact! app kw-dbase0 (fn [_] %))}))
    om/IRender
    (render [_] (construct-component app))))

(om/root view app-state {:target (gdom/getElement "dbase0")})

;; eval this file and open browser with http://localhost:8080/
