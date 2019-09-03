(ns clobanner.core
  (:require
    [cljs.pprint :as pp]
    [clojure.edn :as edn]
    [cljs.core.async :refer [chan put! <! >!]]
    [clobanner.utils :as u])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(declare generate! load-image-file! read!)

;; Initial (sample) banner.
(def banner {:size [1280 670]
             :background ["#6699cc"]
             :texts [[100 150 "Hey" "bold 80px 'monospace'" "#ff0000" "#ffffff" 3]
                     [150 250 "よろしくお願いします。" "bold 40px 'serif'" "#ffffff"]
                     [1100 250 "λ" "300px 'Consolas'" "#63b132"]
                     [100 600 (str \u263a) "bold 180px 'sans-serif'" "#90b4fe" "#000000"]]
             :mime "image/png"})
(set! (.-value (u/dom "banner-declaration"))
      (with-out-str (pp/pprint banner)))

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

;; Keep watching background image file.
(let [ch (chan)
      f (u/dom "bg-image-file")]
  (u/monitor-event f "onchange" ch)
  (go-loop []
    (when-let [ev (<! ch)]
      (load-image-file!)
      (recur))))

;; Keep watching banner declaration.
(let [ch (chan)
      ta (u/dom "banner-declaration")]
  (u/monitor-event ta "oninput" ch)
  (go-loop []
    (when-let [ev (<! ch)]
      (load-image-file!)
      (recur))))

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
  [{:keys [size background texts mime] :as b} img]
  (when b
    (try
      (apply resize! the-canvas size)
      (clear! the-canvas)
      (apply background! the-canvas img background)
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

(put! bg-image-chan :no-image)
