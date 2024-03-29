(ns clobanner.core
  (:require
    [cljs.pprint :as pp]
    [clojure.edn :as edn]
    [cljs.core.async :refer [chan put! <! >!]]
    [clobanner.db :as db]
    [clobanner.utils :as u])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(declare generate! load-image-file! read! save-banner! load-banner! del-banner!)

;; Initial (sample) banner.
(def initial-banner
  {:name "sample1"
   :size [1280 670]
   :background ["#6699cc"]
   :rects [[156 210 28 48 "#66cc44"]
           [160 265 400 10 "#ff0000" "#ffffff88" 8]]
   :texts [[100 150 "Hey" "bold 80px 'monospace'" "#ff0000" "#ffffff" 3]
           [150 250 "よろしくお願いします。" "bold 40px 'serif'" "#ffffff"]
           [1100 250 "λ" "300px 'Consolas'" "#63b132"]
           [100 600 (str \u263a) "bold 180px 'sans-serif'" "#90b4fe" "#000000"]]
   :mime "image/png"})

;; Canvas and composed image.
(defonce the-canvas
  (let [can (u/dom "work-canvas")
        ctx (.getContext can "2d")
        img (u/dom "result-image")]
    {:canvas can :context ctx :image img}))

;; Keep re-generating image.
(defonce bg-image-chan (chan))
(go-loop []
  (when-let [img (<! bg-image-chan)]
    (generate! (read!) img)
    (recur)))

;; Event hooks
(defn- listen!
  [id prop f]
  (let [ch (chan)
        el (u/dom id)]
    (u/monitor-event el prop ch)
    (go-loop []
      (when-let [ev (<! ch)]
        (f)
        (recur)))))
(listen! "bg-image-file" "onchange" load-image-file!)
(listen! "banner-declaration" "onchange" load-image-file!)
(listen! "save-btn" "onclick" save-banner!)
(listen! "del-btn" "onclick" del-banner!)
(listen! "load-btn" "onclick" load-banner!)
(listen! "bak-btn" "onclick" db/backup!)

;; Operations for canvas
(defn- resize!
  [c w h]
  (let [can (:canvas c)]
    (set! (.-width can) w)
    (set! (.-height can) h)))

(defn- size
  [c]
  (let [can (:canvas c)]
    [(.-width can)
     (.-height can)]))

(defn- clear!
  [c]
  (let [ctx (:context c)
        [w h] (size c)]
    (.clearRect ctx 0 0 w h)))

(defn- fill!
  [c color]
  (let [ctx (:context c)
        [w h] (size c)]
    (set! (.-fillStyle ctx) color)
    (.fillRect ctx 0 0 w h)))

(defn- calc-bg-param
  [sx sy sw sh-max dx dy dw dh-max]
  (let [sh (* sw (/ dh-max dw))
        sh (min sh sh-max)
        dh (* dw (/ sh sw))
        dh (min dh dh-max)]
    [sx sy sw sh dx dy dw dh]))

(defn- background!
  ([c img color]
   (let [sw (.-width img)
         [dw dh] (size c)]
     (background! c img color 0 0 sw 0 0 dw dh)))
  ([c img color sx sy]
   (let [sw (- (.-width img) sx)
         [dw dh] (size c)]
     (background! c img color sx sy sw 0 0 dw dh)))
  ([c img color sx sy sw]
   (let [[dw dh] (size c)]
     (background! c img color sx sy sw 0 0 dw dh)))
  ([c img color sx sy sw dx dy]
   (let [[dw dh] (size c)
         dw (- dw dx)
         dh (- dh dy)]
     (background! c img color sx sy sw dx dy dw dh)))
  ([c img color sx sy sw dx dy dw]
   (let [[_ dh] (size c)
         dh (- dh dy)]
     (background! c img color sx sy sw dx dy dw dh)))
  ([c img color sx sy sw dx dy dw dh-max]
   (fill! c color)
   (when (not= img :no-image)
     (let [ctx (:context c)
           sh (-  (.-height img) sy)
           [sx sy sw sh dx dy dw dh]
           (calc-bg-param sx sy sw sh dx dy dw dh-max)]
       (.drawImage ctx img sx sy sw sh dx dy dw dh)))))

(defn- text!
  ([c x y t font-desc color]
   (text! c x y t font-desc color nil nil))
  ([c x y t font-desc color edge]
   (text! c x y t font-desc color edge 1))
  ([c x y t font-desc color edge w]
   (let [ctx (:context c)]
     (set! (.-fillStyle ctx) color)
     (set! (.-font ctx) font-desc)
     (when edge
       (set! (.-strokeStyle ctx) edge)
       (set! (.-lineWidth ctx) w)
       (.strokeText ctx t x y))
     (.fillText ctx t x y))))

(defn- rect!
  ([c x y w h color]
   (rect! c x y w h color nil nil))
  ([c x y w h color edge]
   (rect! c x y w h color edge 1))
  ([c x y w h color edge ew]
   (let [ctx (:context c)]
     (set! (.-fillStyle ctx) color)
     (when edge
       (set! (.-strokeStyle ctx) edge)
       (set! (.-lineWidth ctx) ew)
       (.strokeRect ctx x y w h))
     (.fillRect ctx x y w h))))

(defn- compose!
  [c mime]
  (let [can (:canvas c)
        img (:image c)
        url (.toDataURL can mime)]
    (set! (.-src img) url)))

(defn- toggle-declaration-error!
  [err?]
  (let [bd (u/dom "banner-declaration")]
    (.toggle (.-classList bd) "error" err?)))

(defn- generate!
  [{:keys [size background rects texts mime] :as b} img]
  (when b
    (try
      (apply resize! the-canvas size)
      (clear! the-canvas)
      (apply background! the-canvas img background)
      (dorun (map #(apply rect! the-canvas %) rects))
      (dorun (map #(apply text! the-canvas %) texts))
      (compose! the-canvas mime)
      nil
      (catch :default e
        (toggle-declaration-error! true)
        #_(println (ex-data e))
        nil))))

(defn- load-image-file!
  []
  (if-let [fil (aget (u/dom "bg-image-file") "files" 0)]
    (let [fr (js/FileReader.)
          img (js/Image.)
          ch (chan)]
      (set! (.-textContent (u/dom "image-file-name")) (.-name fil))
      (u/monitor-event fr "onload" ch)
      (u/monitor-event img "onload" ch)
      (go
        (.readAsDataURL fr fil)
        (when-let [ev (<! ch)]
          (set! (.-src img) (aget ev "target" "result"))
          (<! ch)
          (>! bg-image-chan img))))
    (do
      (set! (.-textContent (u/dom "image-file-name")) "")
      (put! bg-image-chan :no-image))))

(defn- read!
  []
  (let [bd (u/dom "banner-declaration")]
    (try
      (toggle-declaration-error! false)
      (edn/read-string (.-value bd))
      ;; TODO: should use spec here?
      (catch :default e
        (toggle-declaration-error! true)
        #_(println (ex-data e))
        nil))))

(defn- reset-banner!
  [banner]
  (when banner
    (set! (.-value (u/dom "banner-declaration"))
          (with-out-str (pp/pprint banner)))
    (put! bg-image-chan :no-image)))

(defn- dir-banners!
  []
  (when-let [names (db/dir)]
    (println names)))

(defn- save-banner!
  []
  (let [banner (read!)]
    (if-let [name (:name banner)]
      (when (js/confirm (str "\"" name "\"をローカルストレージへ保存します。"))
        (db/upsert! name banner)
        (dir-banners!))
      (js/alert "保存するには、:nameキーが必要です。"))))

(defn- del-banner!
  []
  (if-let [name (:name (read!))]
    (when (js/confirm (str "\"" name "\"をローカルストレージから削除します。"))
      (db/remove! name)
      (dir-banners!))
    (js/alert "削除するには、:nameキーが必要です。")))

(defn- load-banner!
  []
  (if-let [name (:name (read!))]
    (when (js/confirm (str "\"" name "\"をローカルストレージからロードします。"))
      (-> name
          db/query
          reset-banner!))
    (js/alert "ロードするには、:nameキーが必要です。")))

(reset-banner! initial-banner)
(dir-banners!)

(comment
  the-canvas
  (:context the-canvas)
  (set! (.-fillStyle (:context the-canvas)) "#ffffff")
  (set! (.-strokeStyle (:context the-canvas)) "#ffffff")
  (.fillRect (:context the-canvas) 5 205 300 150)
  (.fillText (:context the-canvas) "yo" 5 5)
  (js/alert "hey")
  )
