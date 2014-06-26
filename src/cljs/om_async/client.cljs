(ns om-async.client
  (:require [cljs.reader :as reader]
            [goog.events :as events]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            )
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def src "client.cljs")

(enable-console-print!)

(def ^:private http-req-methods
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(def app-state
  (atom {:dbase0 {:name ["employees"] :vals []} ;; TODO create dbase0 by transfer.clj
         ;;:toggle #{nil}
         }))

(defn vals-for-name [owner keyNameX val-keyNameX keyValX]
  (if (= (keyNameX owner) val-keyNameX)
    (keyValX owner)))

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

(defn onClick [owner dbase table column row-value]
  (let [fn-name "onClick"]
    (fn [e]
      (let [idx 0
            data {(u/kw :dbase :name idx) dbase
                  (u/kw :table :name idx) table
                  (u/kw :col :name idx) column
                  (u/kw :row :val idx) row-value}]
        (l/info src fn-name (str "data: " data))
        ;; (l/info src fn-name (str "pr-str owner: " (pr-str owner)))
        (let [toggled-elems (om/get-state owner
                                          :toggle
                                          ;;(:val (:dbase0 app))
                                          )
              isIn (u/contains-value? toggled-elems data)]

          (l/info src fn-name (str "isIn: " isIn))
          ;; (if (isIn)
          ;;   ;; TODO serch if om has some 'state-remove' function
          ;;   (om/set-state! owner :toggle data))
          )

        (edn-xhr
         {:method :put
          :url (str "select/id0")
          :data {:request data}
          :on-complete
          (fn [response]
            ;; (l/info src fn-name (str "Server response: " response))
            )})))))

(defn tr
  "Display table row. dom-cell-elem cound be dom/td or dom/th"
  [owner dbase table column-vals
   dom-cell-elem row-vals css-class]
  ;; (l/info (str src "tr: column-vals: " column-vals))
  (apply dom/tr #js {:className css-class}
         (map #(dom-cell-elem
                #js {:onClick (onClick owner dbase table %1 %2)}
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
  [owner dbase table
   app col-indexes kw dom-table-elem dom-cell-elem alt-row-css-class]
  (let [fn-name "table-elem"]
    ;; (l/info src fn-name (str "(def app " (pr-str app) ")"))
    ;; (l/info src fn-name (str "(rows :name app " col-indexes ")"))
    ;; (l/info src fn-name (str "(rows " kw " app " col-indexes ")"))
    (apply dom-table-elem nil
           (map #(tr owner dbase table %1
                     dom-cell-elem %2 %3)
                ;; TODO do (cycle [nil nil ...]) when processing table header
                (cycle (rows :name app col-indexes))
                (rows kw app col-indexes)
                (cycle ["" alt-row-css-class])))))

(defn display [show] ;; TODO get rid of 'if'
  (if show
    #js {}
    #js {:display "none"}))

(defn create-table-for-columns [toggle owner dbase db-table data col-indexes]
  (let [fn-name "create-table-for-columns"]
    ;; (l/info src fn-name (str "data: " (pr-str data)))
    ;; (l/info (str src "create-table-for-columns: col-indexes: " col-indexes))
    (dom/div nil (str "toggle: " toggle)
             (dom/div nil (dom/table nil
                                     (table-elem owner dbase db-table
                                                 data col-indexes :name dom/thead dom/th "")
                                     (table-elem owner dbase db-table
                                                 data col-indexes :vals dom/tbody dom/td "odd"))))))

(defn get-data [korks parent-data]
  (let [fn-name "get-data"]
    ;; (l/info src fn-name (str "(def parent-data " (pr-str parent-data) ")"))
    ;; (l/info src fn-name (str "(def korks " (pr-str korks) ")"))
    (first (remove nil? (map #(get-in % korks) parent-data)))))

(defn column-filter? [elem-idx] true) ;; no element is filtered out
(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out

(defn create-table [toggle owner dbase db-table
                    tdata]
  (let [fn-name "create-table"]
    ;; (l/info src fn-name (str "owner: " owner))
    ;; (l/info src fn-name (str "dbase: " dbase))
    ;; (l/info src fn-name (str "db-table: " db-table))
    ;; (l/info src fn-name (str "tdata: " (pr-str tdata)))
    (let [all-cols (into [] (range (count tdata)))
          displayed-cols (into [] (filter column-filter? all-cols))]
      (create-table-for-columns toggle owner dbase db-table
                                tdata displayed-cols))))

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

(defn construct-component [app owner {:keys [toggle] :as opts}]
  (let [fn-name "construct-component"]
    (reify
      om/IInitState (init-state [_]
                                (l/info src fn-name (str "init-state"))
                                {:toggle "foo"})
      om/IRenderState
      (render-state [_ {:keys [toggle]}]
                    (let [dbase (first (get-in app [:dbase0 :name]))] ;; (name :kw) => "kw"
                      ;; TODO get rid of 'if'
                      ;; (l/infod src fn-name "dbase" dbase)
                      ;; (l/info src fn-name (str "app: " (pr-str app)))
                      (apply dom/div nil
                             (let [tables (get-in app [:dbase0 :vals])
                                   cnt-tables (count tables)]
                               ;; (l/info src fn-name (str "tables: " (pr-str tables)))
                               ;; (l/info src fn-name (str "cnt-tables: " cnt-tables))
                               (if (= 0 cnt-tables)
                                 (let [msg (str "Fetching data from dbase: " dbase)]
                                   (l/info src fn-name msg)
                                   msg)
                                 (do
                                   (l/info src fn-name (str "Rendering data from dbase: " dbase))
                                   (let [all-tables (into [] (range cnt-tables))
                                         displayed-tables (into [] (filter table-filter? all-tables))]
                                     ;; (l/info src fn-name (str "all-tables: " (pr-str all-tables)))
                                     ;; (l/infod src fn-name "displayed-tables" displayed-tables)
                                     (map #(create-table
                                            toggle
                                            owner
                                            dbase
                                            (get-data [(u/kw-prefix :table %) :name] tables)
                                            (get-data [(u/kw :table :vals  %) :vals] tables))
                                          displayed-tables))
                                   )))))))))

(defn view [app owner]
  (let [fn-name "view"]
    (reify
      om/IWillMount
      (will-mount [_]
                  (l/info src fn-name "will-mount")
                  (edn-xhr
                   {:method :put
                    :url "fetch"
                    ;; :data {:select-rows-from ["employees" "departments"]}
                    ;; :data {:select-rows-from ["departments"]}
                    ;; :data {:show-tables-from ["employees"]}
                    ;; :data {:show-tables-with-data-from [dbase]}
                    :data {:show-tables-with-data-from [(first (get-in app [:dbase0 :name]))]}
                    :on-complete #(om/transact! app [:dbase0 :vals] (fn [_] %))})
                  )
      om/IRenderState
      (render-state [_ {:keys [err-msg]}]
                    (l/info src fn-name "render-state")
                    (om/build construct-component app)
                    ))))


(om/root view app-state {:target (gdom/getElement
                                  "dbase0")}) ;; dbase0 is in index.html

;; eval server.clj, client.cljs, open browser with http://localhost:8080
