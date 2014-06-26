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

(defn kw-from-vec
  "Returns a hash-map from the vector of hash-maps m where the first
  key of the returned hashmap equals to kw"
  [kw vec-of-hash-maps]
  (filter #(= kw (first (keys %)))
          vec-of-hash-maps))

 (defn x [kw vec-of-vals]
   (first
    (vals
     (first
      (kw-from-vec kw vec-of-vals)))))

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


(defn onClick [owner dbase table col row-value]
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
;;         (l/infod src fn-name "data" data)  ;; impossible to work with data

;;         (l/info src fn-name (str "pr-str owner: " (pr-str owner)))
        ;; (l/infod src fn-name "owner" owner)
        (l/infod src fn-name "kw-dbase" kw-dbase)
        (l/infod src fn-name "kw-table" kw-table)
        (l/infod src fn-name "kw-col" kw-col)
        (l/infod src fn-name "kw-row-value" kw-row-value)
        (let [korks [kw-dbase :vals kw-table :vals kw-col :vals]]
          (l/infod src fn-name "korks" korks)
          )
;;         (let [toggled-elems (om/get-state owner
;;                                           :toggle
;;                                           ;;(:val (:dbase0 app))
;;                                           )
;;               isIn (u/contains-value? toggled-elems data)]

;;           (l/info src fn-name (str "isIn: " isIn))
;;           ;; (if (isIn)
;;           ;;   ;; TODO serch if om has some 'state-remove' function
;;           ;;   (om/set-state! owner :toggle data))
;;           )

;;         (edn-xhr
;;          {:method :put
;;           :url (str "select/id0")
;;           :data {:request data}
;;           :on-complete
;;           (fn [response]
;;             ;; (l/info src fn-name (str "Server response: " response))
;;             )})
        ))))

(defn tr
  "Display table row. dom-cell-elem cound be dom/td or dom/th"
  [owner dbase table column-vals
   dom-cell-elem row-vals css-class]
  (let [fn-name "tr"]
    ;;(l/infod src fn-name "table" table)
    (l/infod src fn-name "column-vals" column-vals)
    ;; (l/infod src fn-name "dom-cell-elem" dom-cell-elem)
    (l/infod src fn-name "row-vals" row-vals)
    (apply dom/tr #js {:className css-class}
           (map #(dom-cell-elem
                  ;;#js {:onClick (onClick owner dbase table %1 %2)}
                  nil
                  %2)
                column-vals
                row-vals

                ))))

(defn key-vector [indexes]
  (into [] (map #(u/kw :col nil %) indexes)))

(defn rows [kw app col-indexes]
  (into []
  (map #(x % app)
       (key-vector col-indexes))))

(defn table-elem
  [owner dbase table
   data col-indexes kw dom-table-elem dom-cell-elem alt-row-css-class]
  (let [fn-name "table-elem"]
    (l/infod src fn-name "data" data)
    (l/infod src fn-name "col-indexes" col-indexes)
    (l/infod src fn-name "kw" kw)
    (apply dom-table-elem nil
           (map #(tr owner dbase table %1
                     dom-cell-elem %2 %3)
                ;; TODO do (cycle [nil nil ...]) when processing table header
                (cycle    (rows :name data col-indexes))
                (identity (rows kw    data col-indexes))
                (cycle ["" alt-row-css-class])))))

(defn display [show] ;; TODO get rid of 'if'
  (if show
    #js {}
    #js {:display "none"}))

(defn create-table-for-columns [toggle owner dbase db-table data col-indexes]
  (let [fn-name "create-table-for-columns"]
    (l/infod src fn-name "data" data)
    ;; (l/info (str src "create-table-for-columns: col-indexes: " col-indexes))
    (dom/div nil (str "toggle: " toggle)
             (dom/div nil (dom/table nil
                                     (table-elem owner dbase db-table
                                                 data col-indexes :name dom/thead dom/th "")
                                     (table-elem owner dbase db-table

                                                 data col-indexes :vals dom/tbody dom/td "odd"))))))

(def korks [:table0 :name])
(def parent-data
  [{:table0 {:vals [{:col0 {:vals ["Customer Service" "Development"], :name ["dept_name"]}}
                    {:col1 {:vals ["d009" "d005"], :name ["dept_no"]}}], :name ["departments"]}}
   {:table1 {:vals [{:col0 {:vals ["9999-01-01" "9999-01-01"], :name ["to_date"]}} {:col1 {:vals ["1986-06-26" "1996-08-03"], :name ["from_date"]}} {:col2 {:vals ["d005" "d007"], :name ["dept_no"]}} {:col3 {:vals ["10001" "10002"], :name ["emp_no"]}}], :name ["dept_emp"]}} {:table2 {:vals [{:col0 {:vals ["1991-10-01" "9999-01-01"], :name ["to_date"]}} {:col1 {:vals ["1985-01-01" "1991-10-01"], :name ["from_date"]}} {:col2 {:vals ["110022" "110039"], :name ["emp_no"]}} {:col3 {:vals ["d001" "d001"], :name ["dept_no"]}}], :name ["dept_manager"]}} {:table3 {:vals [{:col0 {:vals ["1986-06-26" "1985-11-21"], :name ["hire_date"]}} {:col1 {:vals ["M" "F"], :name ["gender"]}} {:col2 {:vals ["Facello" "Simmel"], :name ["last_name"]}} {:col3 {:vals ["Georgi" "Bezalel"], :name ["first_name"]}} {:col4 {:vals ["1953-09-02" "1964-06-02"], :name ["birth_date"]}} {:col5 {:vals ["10001" "10002"], :name ["emp_no"]}}], :name ["employees"]}} {:table4 {:vals [{:col0 {:vals ["1987-06-26" "1988-06-25"], :name ["to_date"]}} {:col1 {:vals ["1986-06-26" "1987-06-26"], :name ["from_date"]}} {:col2 {:vals ["60117" "62102"], :name ["salary"]}} {:col3 {:vals ["10001" "10001"], :name ["emp_no"]}}], :name ["salaries"]}} {:table5 {:vals [{:col0 {:vals ["9999-01-01" "9999-01-01"], :name ["to_date"]}} {:col1 {:vals ["1986-06-26" "1996-08-03"], :name ["from_date"]}} {:col2 {:vals ["Senior Engineer" "Staff"], :name ["title"]}} {:col3 {:vals ["10001" "10002"], :name ["emp_no"]}}], :name ["titles"]}}])
;;(def korks [:table0 :vals])


 (defn get-data [kw-table kw-col kw-name-or-vals parent-data]
   (let [fn-name "get-data"]
     (l/infod src fn-name "kw-table" kw-table)
     (l/infod src fn-name "kw-col" kw-col)
     (l/infod src fn-name "kw-name-or-vals" kw-name-or-vals)
     (l/infod src fn-name "parent-data" parent-data)
     ;;(first (remove nil? (map #(get-in % korks) parent-data))))
     (let [m (first
              (remove nil?
                      (into []
                            (map #(get-in % [kw-table :vals]) parent-data))))]
       (l/infod src fn-name "m" m)
       (map #(get-in % [kw-col kw-name-or-vals]) m)
       ;; (map #(get-in % [:col0 :vals]) m)
       )
     ))


(def kw-table :table0)
(def kw-col :col0)
(def kw-name-or-vals :vals)

(get-data kw-table kw-col kw-name-or-vals parent-data)



(defn column-filter? [elem-idx] true) ;; no element is filtered out
;; (defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out
(defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no element is filtered out

(defn create-table [toggle owner dbase db-table
                    data]
  (let [fn-name "create-table"]
;;     (l/infod src fn-name "owner" owner)
;;     (l/info src fn-name (str "owner: " (pr-str owner)))
    ;; (l/info src fn-name (str "dbase: " dbase))
    ;; (l/infod src fn-name "db-table" db-table)
    (l/infod src fn-name "data" data)
    (let [all-cols (into [] (range (count data)))
          displayed-cols (into [] (filter column-filter? all-cols))]
      ;; (l/infod src fn-name "all-cols" all-cols)
      (create-table-for-columns toggle owner dbase db-table
                                data displayed-cols))))

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
    ;; (l/infod src fn-name "owner" owner)
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
                               (l/info src fn-name (str "tables: " (pr-str tables)))
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
                                            (get-data (u/kw-prefix :table %) :col0 :name tables)
                                            (get-data (u/kw-prefix :table %) :col0 :vals tables))
                                          displayed-tables))
                                   )))))))))

(defn view [app owner]
  (let [fn-name "view"]
    ;; (l/infod src fn-name "owner" owner)
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
