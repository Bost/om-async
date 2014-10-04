(ns om-async.newclient
;;   (:use [jayq.core :only [$ css html document-ready]])
  (:require [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [om.dom :as orig-dom :include-macros true]
            ;; this is probably not needed at the moment
            [cljs.core.async :as async :refer [put! chan <!]]
            [om-async.utils :as u]
            [om-async.logger :as l]
            [om-async.onclick :as oc]
            [om-async.cli-transform :as t]
            )
  (:import [goog.net XhrIo]
           [goog.ui TableSorter])
  (:require-macros [om-async.logger :as l]
                   ;; defschema is needed by defcomponent
                   [schema.macros :refer [defschema]]))

(enable-console-print!)

(def app-state (atom {}))

(defcomponent render-single [item owner]
  (did-mount [this]
             (let [n (om/get-node owner)]
               (println (str "render-single: owner-id: " (.-id n)))))
  (render-state [_ state]
                (dom/li {:id (str "li-id-" item) :style {:color "red"}}
                        (str "Item " item))
;;                 (dom/div
;;                  (dom/h4 "render-single"))
                 ))

(defcomponent render-multi [{:keys [items sort]} owner]
  (render-state [_ state]
                ;; [_ {:keys [err-msg]}]
                (dom/div
                 (dom/h3 "render-multi")
                 (render-single (first items) {}))))

(defcomponent view [app owner]
  (did-mount [this]
             (let [n (om/get-node owner)]
               (println (str "view: owner-id: " (.-id n)))))
  (render-state [this state]
                (dom/div {:id "div-id"}
                         (dom/h2 {:id "div-h2-id"} "view-h2")
                         (dom/div {:id "div-div-id"}
                                  (dom/ul {:id "div-div-ul-id" :class "a-list"}
                                          (for [i (range 10)]
                                            (om/build render-single i)
                                            )))
                         )))

(om/root view app-state {:target (gdom/getElement "dbase0")})
