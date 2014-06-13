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
  (let [data (db/data)]
    (generate-response data)))

(defroutes routes
  (GET "/" [] (index))
  (GET "/classes" [] (classes))
  (PUT "/class/:id/update"
       {params :params edn-params :edn-params}
;;        (update-class (:id params) edn-params)
       )
  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))
