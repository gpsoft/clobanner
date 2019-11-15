(ns clobanner.utils
  (:require [cljs.core.async :refer [put!]]))

(defn d [e] (prn e) e)

(defn dom
  [id]
  (.getElementById js/document id))

(defn monitor-event
  [target prop ch]
  (aset target prop #(put! ch %)))

(defn base64encode
  [s]
  ;; Not sure it's correct.
  (-> s
      (js/encodeURIComponent)
      (js/unescape)
      (js/btoa)))
