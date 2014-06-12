(ns om-async.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            ;; [datomic.api :as d]
            [om-async.db :as db]
            ))

(defn x []
  (:gender (nth
            (into [] (db/print-users))
            1)))

;;(x)

;; (def uri "datomic:free://localhost:4334/om_async")
;; (def conn (d/connect uri))

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))
(defn generate-response [data & [status]]
;;   (println "(pr-str data)" (pr-str data))
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

;; (defn update-class [id params]
;;   (let [db    (d/db conn)
;;         title (:class/title params)
;;         eid   (ffirst
;;                 (d/q '[:find ?class
;;                        :in $ ?id
;;                        :where
;;                        [?class :class/id ?id]]
;;                   db id))]
;;     (d/transact conn [[:db/add eid :class/title title]])
;;     (generate-response {:status :ok})))


(defn classes []
  (let [
        ;; db (d/db conn)
        classes
[{:class/title (str (x) " The Structure and Interpretation of Computer Programs")
  :class/id "6001",
  :class/professor {:db/id 17592186045426},
  :class/students
    #{{:db/id 17592186045428}
      {:db/id 17592186045429}
      {:db/id 17592186045430}
      {:db/id 17592186045432}},
  :db/id 17592186045422}
;;  {:class/title "Linear Algebra",
;;   :class/id "1806",
;;   :class/professor {:db/id 17592186045427},
;;   :class/students
;;     #{{:db/id 17592186045428}
;;       {:db/id 17592186045429}
;;       {:db/id 17592186045431}},
;;   :db/id 17592186045424}
;;  {:class/title "The Structure and Interpretation of Classical Mechanics",
;;   :class/id "6946",
;;   :class/professor {:db/id 17592186045425},
;;   :class/students
;;   #{{:db/id 17592186045428}
;;     {:db/id 17592186045430}
;;     {:db/id 17592186045431}
;;     {:db/id 17592186045433}},
;;   :db/id 17592186045423}
 ]

;;         (vec (map #(d/touch (d/entity db (first %)))
;;                (d/q '[:find ?class
;;                       :where
;;                       [?class :class/id]]
;;                  db)))

        ]
;;     (println "classes" classes)
;;     (pr-str "pr-str classes" classes)
    (generate-response classes)))

(defroutes routes
  (GET "/" [] (index))
  (GET "/classes" [] (classes))
  (PUT "/class/:id/update"
    {params :params edn-params :edn-params}
    (update-class (:id params) edn-params))
  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))
