(ns om-async.server
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            ;; [datomic.api :as d]
            [om-async.transform :as trans]
            [om-async.logger :as l]
            ))

;; Functions for Ring here. TODO consider moving them to ring.cljs

(def src "server.clj")

(defn response [data & [status]]
  ;; (l/info src "response" (str "(pr-str data): " (pr-str data)))
  {:status (or status 200) ;; Status code: 200 'OK (The request was fulfilled)'
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes routes
  (GET "/"
       []
       (file-response "public/html/index.html" {:root "resources"}))

  (PUT "/fetch"
       {params :params edn-params :edn-params}
       (let [data (trans/fetch edn-params)]
         (response data)))

  (PUT "/select/:id"
       {params :params edn-params :edn-params}
       ;; (l/info src "PUT/select" (str "id: " (:id params)))
       (let [data (trans/request edn-params)]
         ;; (l/info src "PUT/select" (str "data: " data))
         (response (merge data {:status :ok}))))

  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))
