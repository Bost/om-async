(ns om-async.server
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
  {:status (or status 200) ;; Status code: 200 'OK (The request was fulfilled)'
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn update-class [id params]
  (println "server.clj: update-class: " id params)
;;   (let [db    (d/db conn)
;;         title (:class/title params)
;;         eid   (ffirst
;;                 (d/q '[:find ?class
;;                        :in $ ?id
;;                        :where
;;                        [?class :class/id ?id]]
;;                   db id))]
;;     (d/transact conn [[:db/add eid :class/title title]])
    (generate-response {:status :ok})
  )

(defn classes []
  (println "server.clj: this fn shouldn't be invoked!" )
  (let [data (db/fetch nil)]
    (generate-response data)))

(defn fetch [edn-params]
;;   (println "server.clj: " edn-params)
  (let [data (db/fetch edn-params)]
    (generate-response data)))

(defroutes routes
  (GET "/" [] (index))
  (GET "/classes" [] (classes))
  (PUT "/fetch"
       {params :params edn-params :edn-params}
       (fetch edn-params))

  (PUT "/class/:id/update"
       {params :params edn-params :edn-params}
       (update-class (:id params) edn-params))
  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))
