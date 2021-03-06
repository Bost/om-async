(ns om-async.server
  (:require [ring.util.response :refer [file-response]]
            ;; the ring adapter is a configurable layer between the web app and the web server
            ;; possible options: 1. jetty 2. servlets 3. server running outside of the JVM
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.edn :refer [wrap-edn-params]]

            ;; compojure is used for dispatching
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            ;; [datomic.api :as d]
            [om-async.transform :as trans]
            [om-async.logger :as l]
            [onelog.core :as log]
            [clojure.pprint :as pp] ; for debug purposes
            ))

;; Functions for Ring here. TODO consider moving them to ring.cljs

(def src "server.clj")

(defn response [data & [status]]
  ;; (l/info src "response" (str "(pr-str data): " (pr-str data)))
  {:status (or status 200) ;; Status code: 200 'OK (The request was fulfilled)'
   :headers {"Content-Type" "application/edn; charset=UTF-8"}
   :body (pr-str data)})

(defroutes app-routes
  (GET "/"
       []
       (file-response "public/html/index.html" {:root "resources"}))

  (PUT "/fetch-dbases"
       {params :params edn-params :edn-params}
       (let [fn-name "routes-PUT-fetch-dbases"]
         (l/infod src fn-name "edn-params" edn-params)
         (l/infod src fn-name "params" params)
         (let [data (trans/fetch-dbases edn-params)]
           (l/infod src fn-name "data" data)
           (response data))))

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
         (l/infod src fn-name "params" params)
         (let [data (trans/request (:request params))
               ;; TODO server.clj add {:status :ok} to response; onclick.cljs extracts ite
               ;; data (merge data {:status :ok})
               r (response data)
               ]
           (l/infod src fn-name "r" r)
           r)))
  (route/files "/" {:root "resources/public"}))

(def app
  (wrap-edn-params app-routes))

;; comment this out if the server is started by lein ring server
;; (defonce server
;;   (fn [request]
;;     ;; (log/start!)
;;     (run-jetty
;;      #'app {:port 8080
;;             ;; blocks the thread until server ends (defaults to true)
;;             :join? false})
;;     ;; (log/info+ "Server started")
;;     ))

;; clean the REPL - works only in clojure not in clojurescript
;; (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))
