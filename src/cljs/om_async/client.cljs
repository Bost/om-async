(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-async.utils :as utils]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

;; (println "Hello world! Jim")

(defn tr [dom-cell-elem rows css-class]
  (apply dom/tr #js {:className css-class}
         (map #(dom-cell-elem nil %) rows)))

(defn create-key-vector [indexes]
  (into [] (map #(utils/column-keyword %) indexes)))

(defn rows [key-name app cols]
  (apply map vector
         (map #(key-name (% app))
              (create-key-vector cols))))

(defn table-elem
  [app cols kw-row-vals dom-table-elem dom-cell-elem alt-row-css-class]
  (apply dom-table-elem nil
         (map #(tr dom-cell-elem %1 %2)
              (rows kw-row-vals app cols)
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
  (atom {:classes []}))

(defn display [show]
  ;; TODO get rid of 'if'
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
       (println (str "client.cljs: server response: " res)))}))

;; (on-edit "my-id" "my-title")

(defn table [data cols]
  (println (str "client.cjs: table: " (pr-str data)))
  (dom/table nil
             (table-elem data cols :col-name dom/thead dom/th "")
             (table-elem data cols :col-vals dom/tbody dom/td "odd")))

;; "dbase-data: data from/for multiple tables; table-index: table index to display"
(defn table-data [dbase-data table-index]
  (println (str "client.cljs: table-data: dbase-data: " (pr-str dbase-data)))
  (let [table-kw (utils/table-keyword table-index)
        ret-val (table-kw dbase-data)]
    (println (str "client.cljs: table-data: table-kw: " table-kw))
    (println (str "client.cljs: table-data: ret-val: " (pr-str ret-val)))
    ret-val))

;; (println (pr-str [1 2 3 4]))

;; TODO do not crash when too many columns reqested;
;; or intelligently display only available columns
(defn component-constructor [app cols]
  ;; TODO get rid of 'if'
  (apply dom/div nil
         (let [content (identity
                        ;;first ;; dealing with multiple dbases
                        (:classes app))]
           (println (str "client.cljs: component-constructor: content: " (pr-str content)))
           (if (= 0 (count content))
             "Fetching data db ..."
             (let [table-data-idx
                   ;;(table-data content 0)
                   content
                   all-cols (into [] (range (count table-data-idx)))]
               (println (str "client.cljs: all-cols: " all-cols))
               (map #(table % cols) table-data-idx))))))

(defn classes-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_] (edn-xhr
                     {:method :put
                      ;; :url "classes"
                      :url "fetch"
;;                       :data {:select-rows-from ["employees" "departments"]}
                      :data {:select-rows-from ["departments"]}
;;                       :data {:show-tables-from ["employees"]}
;;                       :data {:show-tables-with-data-from ["employees"]}
                      :on-complete #(om/transact! app :classes (fn [_] %))}))
    om/IRender
    (render [_] (component-constructor app
                                       [0] ;:col2 :col3 :col4 :col5
                                       ))))

(om/root classes-view app-state
  {:target (gdom/getElement "classes")})

;; open http://localhost:8080/
