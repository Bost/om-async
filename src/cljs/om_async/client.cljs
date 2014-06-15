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
       (println (str "client.cljs; server response: " res)))}))

;; (on-edit "my-id" "my-title")

(defn construct-table [data cols]
  ;; (println (str "client.cjs; construct-table: " (pr-str data)))
  (dom/table nil
             (table-elem data cols :col-name dom/thead dom/th "")
             (table-elem data cols :col-vals dom/tbody dom/td "odd")))

(defn get-data [kw parent-data]
  ;; (println (str "client.cljs; get-data: parent-data: " (pr-str parent-data)))
  (let [child-data (map kw parent-data)]
    ;; (println (str "client.cljs; get-data: kw: " kw))
    ;; (println (str "client.cljs; get-data: child-data: " (pr-str child-data)))
    (into [] child-data)))

;; TODO do not crash when too many columns reqested;
;; or intelligently display only available columns
(defn construct-component [app]
  ;; TODO get rid of 'if'
  ;; (println (str "client.cljs; construct-component: app: " (pr-str app)))
  (apply dom/div nil
         (let [db-data (:classes app)]
           ;; (println (str "client.cljs; component-constructor: db-data: " (pr-str db-data)))
           (if (= 0 (count db-data))
             "Fetching data from the dbase... "
             (let [tables (get-data (utils/table-keyword 0) db-data)
                   tables-count (count tables)]
               ;; (println (str "client.cljs; tables-count: " tables-count))
               ;; (println (str "client.cljs; tables: " tables))
               (let [count-columns (map count tables)]
                 ;; (println (str "client.cljs; count-columns: " count-columns))
                 (map #(construct-table
                        %1
                        ;; TODO filter this to hide some columns
                        (into [] (range %2)))
                      tables
                      count-columns)))))))

(defn classes-view [app owner]
  (reify
    om/IWillMount
    (will-mount [_] (edn-xhr
                     {:method :put
                      ;; :url "classes"
                      :url "fetch"
;;                       :data {:select-rows-from ["employees" "departments"]}
;;                       :data {:select-rows-from ["departments"]}
;;                       :data {:show-tables-from ["employees"]}
                      :data {:show-tables-with-data-from ["employees"]}
                      :on-complete #(om/transact! app :classes (fn [_] %))}))
    om/IRender
    (render [_] (construct-component app))))

(om/root classes-view app-state
  {:target (gdom/getElement "classes")})

;; open http://localhost:8080/
