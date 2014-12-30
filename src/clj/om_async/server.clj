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
       (let [fn-name "routes-PUT-fetch"]
         (l/infod src fn-name "edn-params" edn-params)
         (l/infod src fn-name "params" params)
         (let [data (trans/fetch edn-params)]
           (l/infod src fn-name "data" data)
           (response data))))

  (PUT "/select/:id"
       {params :params edn-params :edn-params}
       (let [fn-name "routes-PUT-select-id"]
         ;; (l/infod src fn-name "id" (:id params))
         (l/infod src fn-name "edn-params" edn-params)
         ;; (l/infod src fn-name "params" params)
         (let [
               r (trans/request (:request params))
               data {:response (:request edn-params)
                     :TODO "db.clj: search in all entities for :column with given :value"}
               ]
           (l/infod src fn-name "r" r)
           (l/infod src fn-name "data " data)
           (response (merge data {:status :ok})))
         ))

  (route/files "/" {:root "resources/public"}))

(def app
  (-> routes
      wrap-edn-params))

(defonce server
  (run-jetty #'app {:port 8080 :join? false}))

;; clean the REPL - works only in clojure not in clojurescript
;; (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))

