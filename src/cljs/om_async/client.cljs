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

(def ^:private http-req-methods
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

;; TODO change vectors to hash-maps
(def app-state
  (atom {
;;          :dbase0 {}
         :dbase0 {:toggle {:in "in-val"} :name ["employees"] :vals []} ;; TODO create dbase0 by transfer.clj
;;          :toggle #{nil}
         }))

(defn filter-kw
  "Returns a hash-map from the vector of hash-maps m where the first
  key of the returned hashmap equals to kw"
  [kw vec-of-hash-maps]
  (filter #(= kw (first (keys %)))
          vec-of-hash-maps))

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
            ))

;;         (edn-xhr
;;          {:method :put
;;           :url (str "select/id0")
;;           :data {:request data}
;;           :on-complete
;;           (fn [response]
;;             ;; (l/info src fn-name (str "Server response: " response))
;;             )})
        ))))

(defn to-korks [f values]
  (let [r (map-indexed (fn [idx value] {(f idx) value}) values)
        rv (into [] r)]
    (apply merge rv)))

(defn convert-to-korks [f1 f2 vals-vec]
  (let [fn-name "convert-to-korks"]
    ;; (l/infod src fn-name "vals-vec" vals-vec)
    (let [xs (map (fn [x] (to-korks f1 x)) vals-vec)
          xs-seq (into [] xs)
          ys (to-korks f2 xs-seq)]
      ;; (l/infod src fn-name "xs" xs)
      ;; (l/infod src fn-name "xs-seq" xs-seq)
      ;; (l/infod src fn-name "ys" ys)
      ys)))

(defn row-vals-to-korks [vals-vec]
  (convert-to-korks u/kw-col u/kw-row vals-vec))

(defn col-vals-to-korks [vals-vec]
  (convert-to-korks u/kw-row u/kw-col vals-vec))

;; (def row-vec [["row-0-col0" "row-0-col-1" "row-0-col2"]])
;; (row-vals-to-korks row-vec)
;; {:row0 {:col0 row-0-col0, :col1 row-0-col-1, :col2 row-0-col2}}

;; (def col-vec [["row-0-col0" "row-1-col-0" "row-2-col0"]])
;; (col-vals-to-korks col-vec)
;; {:col0 {:row0 row-0-col0, :row1 row-1-col-0, :row2 row-2-col0}}

(def row-vals ["Customer Service" "d009"])
(row-vals-to-korks row-vals)
(to-korks u/kw-col row-vals)
(def in-row-vals {:col0 "Customer Service", :col1 "d009"})
(def i in-row-vals)

(defn tr
  "Display table row. dom-cell-elem cound be dom/td or dom/th"
  [owner dbase table
   column-vals dom-cell-elem row-vals css-class]
  (let [fn-name "tr"
        in-row-vals (to-korks u/kw-col row-vals)
        new-row-vals (into [] (vals in-row-vals))
        ]
    (l/info src fn-name "----------")
    (l/infod src fn-name "table" table)
    (l/infod src fn-name "column-vals" column-vals)
    (l/infod src fn-name "dom-cell-elem" dom-cell-elem)
    (l/infod src fn-name "row-vals" row-vals)
    (l/infod src fn-name "new-row-vals" new-row-vals)
    (if (not= new-row-vals row-vals)
      (l/info src fn-name "(not= new-row-vals row-vals)"))
    (apply dom/tr #js {:className css-class}
           (map #(dom-cell-elem
                  #js {:onClick (onClick owner dbase table %1 %2)}
                  %2)
                column-vals
                row-vals
                ))))

(defn get-table-data [vec-of-hash-maps korks]
  (let [fn-name "get-table-data"]
    (l/info src fn-name "----------")
    (l/infod src fn-name "vec-of-hash-maps" vec-of-hash-maps)
    (l/infod src fn-name "korks" korks)
    ;; (l/info src fn-name "(get-table-data vec-of-hash-maps korks)")
    (let [kw (first korks)
          m (filter-kw kw vec-of-hash-maps)
          r (get-in (first m) korks)]
      ;; (l/infod src fn-name "kw" kw)
      ;; (l/infod src fn-name "m" m)
      (l/infod src fn-name "r" r)
      r)))

(defn key-vector [indexes]
  (into [] (map #(u/kw :col nil %) indexes)))

(defn rows [kw data col-indexes]
  (let [fn-name "rows"]
    (l/infod src fn-name "kw" kw)
    (l/infod src fn-name "data" data)
    (l/infod src fn-name "col-indexes" col-indexes)
    (let [v (into []
                  (map #(get-table-data data [% kw])
                       (key-vector col-indexes)))
          f (fn [& args] (into [] args))
          r (apply map f v)
          ]
      ;; (l/infod src fn-name "v" v)
      (l/infod src fn-name "r" r)
      r)))

(defn table-elem
  [owner dbase table
   data col-indexes kw dom-table-elem dom-cell-elem alt-row-css-class]
  (let [fn-name "table-elem"]
;;     (l/infod src fn-name "data" data)
;;     (l/infod src fn-name "col-indexes" col-indexes)
;;     (l/infod src fn-name "kw" kw)
;;     (l/info  src fn-name "(rows :name data col-indexes)")
;;     (l/info  src fn-name "(rows kw data col-indexes)")
    (let [row-vals (rows :name data col-indexes)]
      (l/infod src fn-name "row-vals" row-vals)
      (apply dom-table-elem nil
             (map #(tr owner dbase table %1 dom-cell-elem %2 %3)
                  ;; TODO do (cycle [nil nil ...]) when processing table header
                  (cycle row-vals) ;; %1
                  (rows kw    data col-indexes)
                  (cycle ["" alt-row-css-class])
                  )))))


(defn display [show] ;; TODO get rid of 'if'
  (if show
    #js {}
    #js {:display "none"}))

(defn create-table-for-columns [toggle owner dbase db-table data col-indexes]
  (let [fn-name "create-table-for-columns"]
    ;; (l/infod src fn-name "data" data)
    ;; (l/info (str src "create-table-for-columns: col-indexes: " col-indexes))
    (dom/div nil
             ;;(str "toggle: " toggle)
             db-table
             (dom/div nil (dom/table nil
                                     (table-elem owner dbase db-table
                                                 data col-indexes :name dom/thead dom/th "")
                                     (table-elem owner dbase db-table
                                                 data col-indexes :vals dom/tbody dom/td "odd")
                                     )))))

(defn column-filter? [elem-idx] true) ;; no element is filtered out
(defn table-filter?  [elem-idx] true) ;; (= elem-idx 0) ;; true = no element is filtered out
;; (defn table-filter?  [elem-idx] (= elem-idx 0)) ;; true = no element is filtered out

(defn create-table [toggle owner dbase db-table data]
  (let [fn-name "create-table"]
    ;; (l/infod src fn-name "dbase" dbase)
    ;; (l/infod src fn-name "db-table" db-table)
    ;; (l/infod src fn-name "data" data)
    (let [all-cols       (into [] (range (count data)))
          displayed-cols (into [] (filter column-filter? all-cols))]
      ;; (l/infod src fn-name "displayed-cols" displayed-cols)
      (create-table-for-columns toggle owner dbase db-table
                                data displayed-cols))))

(defn old-render-data [data owner dbase cnt-tables toggle]
  (let [fn-name "old-render-data"]
    (l/infod src fn-name "data" data)
    (l/info src fn-name (str "Rendering data from dbase: " dbase))
    (let [all-tables       (into [] (range cnt-tables))
          displayed-tables (into [] (filter table-filter? all-tables))
          vec-of-hash-maps (get-in data [:dbase0 :vals])
          ]
      ;; (l/infod src fn-name "all-tables" all-tables)
      ;; (l/infod src fn-name "displayed-tables" displayed-tables)
      (map #(create-table toggle owner dbase
                          (first
                           (get-table-data vec-of-hash-maps
                                           [(u/kw-prefix :table %) :name]))
                          (identity
                           (get-table-data vec-of-hash-maps
                                           [(u/kw-prefix :table %) :vals])))
           displayed-tables))))

(defn render-row [row]
  (apply dom/tr nil
         (map #(dom/td nil (str %)) row)))

(defn render-table [tname tdata]
  (let [fn-name "render-table"]
    (l/infod src fn-name "tdata" tdata)
    (let [header (into [] (keys (first tdata)))
          rows   (into [] (map #(into [] (vals (nth tdata %)))
                               (range (count tdata))))]
      (l/infod src fn-name "tname" tname)
      (l/infod src fn-name "header" header)
      (l/infod src fn-name "rows" rows)
      (dom/div nil
               tname
               (dom/div nil
                        (dom/table nil
                                   (dom/thead nil
                                              (apply dom/tr nil
                                                     (map #(dom/th nil (str %)) header)))
                                   (apply dom/tbody nil
                                          (map render-row rows))))))))

(defn render-data [data owner]
  (let [fn-name "render-data"]
    (l/infod src fn-name "data" data)
    (let [tname (first (get-in data [:dbase0 :name]))
          tdata (get-in data [:dbase0 :vals])]
      (render-table tname tdata))))

(defn construct-component [data owner {:keys [toggle] :as opts}]
  (let [fn-name "construct-component"]
    (l/infod src fn-name "data" data)
    ;; (l/infod src fn-name "owner" owner)
    (reify
      om/IInitState (init-state [_]
                                (l/infod src fn-name "_" _)
;;                                 {:toggle "foo"}
;;                                 {:dbase0 {:name ["employees"] :vals ["init-val"]}}
                                {:dbase0 {:toggle {:in "in-init-val"}}}
                                )
      om/IRenderState
      (render-state [_ {:keys [toggle]}]
                    (let [dbase (first (get-in data [:dbase0 :name]))]
                      ;; TODO get rid of 'if'
                      ;; (l/infod src fn-name "dbase" dbase)
                      (dom/div nil
                             (let [tables (get-in data [:dbase0 :vals])
                                   cnt-tables (count tables)]
                               ;; (l/infod src fn-name "tables" tables)
                               ;; (l/infod src fn-name "cnt-tables" cnt-tables)
                               (if (= 0 cnt-tables)
                                 (let [msg (str "Fetching data from dbase: " dbase)]
                                   (l/info src fn-name msg)
                                   msg)
                                 ;; (render-data data owner dbase cnt-tables toggle)
                                 (render-data data owner))
                               )

                             ))))))

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
                    :data {:select-rows-from [{:dbase "employees" :table "departments" :idx 0}
                                              {:dbase "employees" :table "employees"   :idx 1}]}
;;                     :data {:select-rows-from [{:dbase "employees" :table "departments" :idx 0}]}
;;                     :data {:show-tables-from ["employees"]}
                    ;; :data {:show-tables-with-data-from [dbase]}
;;                     :data {:show-tables-with-data-from [(first (get-in data [:dbase0 :name]))]}
                    :on-complete #(om/transact! data [:dbase0 :vals] (fn [_] %))})
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

