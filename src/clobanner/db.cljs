(ns clobanner.db
  (:require
    [clojure.edn :as edn]
    [clobanner.utils :as u]))

(def ^:private ls-key "jp.dip.gpsoft.clobanner.db")

(defn- the-db
  []
  (if-let [db-str (.getItem js/localStorage ls-key)]
    (edn/read-string db-str)
    {}))

(defn dir
  []
  (let [db (the-db)]
    (keys db)))

(defn query
  [name]
  (let [db (the-db)]
    (get-in db [name])))

(defn upsert!
  [name banner]
  (let [db (the-db)]
    (->> banner
         (assoc-in db [name])
         prn-str
         (.setItem js/localStorage ls-key))))

(defn remove!
  [name]
  (let [db (the-db)]
    (->> (dissoc db [name])
         prn-str
         (.setItem js/localStorage ls-key))))

(defn backup!
  []
  (when-let [db-str (.getItem js/localStorage ls-key)]
    (let [data (u/base64encode db-str)
          href (str "data:application/edn;charset=utf-8;base64," data)
          a (.createElement js/document "a")]
      (.setAttribute a "download" "clobanner.edn")
      (.setAttribute a "href" href)
      (.appendChild (u/dom "local-storage") a)
      (.click a)
      (.remove a))))
