(ns clobanner.core
  (:require
    [cljs.pprint :as pp]
    [clojure.edn :as edn]
    [cljs.core.async :refer [chan <! >!]]
    [clobanner.utils :as u])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(declare generate! load-image-file! read!)

;; Initial (sample) banner.
(def banner {:size [1280 670]
             :background ["#6699cc" 0 30]
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

(defn- background!
  [c img color ix iy]
  (if (= img :no-image)
    (fill! c color)
    (let [ctx (:context c)
          [w h] (size c)
          iw (- (.-width img) ix)
          ih (* iw (/ h w))]
      (.drawImage ctx img ix iy iw ih 0 0 w h))))

(defn- text!
  ([c x y t font-desc color]
   (text! c x y t font-desc color nil nil))
  ([c x y t font-desc color edge]
   (text! c x y t font-desc color edge 1))
  ([c x y t font-desc color edge w]
   (let [ctx (:context c)]
     (set! (.-fillStyle ctx) color)
     (set! (.-font ctx) font-desc)
     (.fillText ctx t x y)
     (when edge
       (set! (.-strokeStyle ctx) edge)
       (set! (.-lineWidth ctx) w)
       (.strokeText ctx t x y)))))

(defn- compose!
  [c mime]
  (let [can (:canvas c)
        img (:image c)
        url (.toDataURL can mime)]
    (set! (.-src img) url)))

(defn- generate!
  [{:keys [size background texts mime] :as b} img]
  (when b
    (apply resize! the-canvas size)
    (clear! the-canvas)
    (apply background! the-canvas img background)
    (dorun (map #(apply text! the-canvas %) texts))
    (compose! the-canvas mime)))

(defn- load-image-file!
  []
  (if-let [fil (aget (u/dom "bg-image-file") "files" 0)]
    (let [fr (js/FileReader.)
          img (js/Image.)
          ch (chan)]
      (u/monitor-event fr "onload" ch)
      (u/monitor-event img "onload" ch)
      (go
        (.readAsDataURL fr fil)
        (when-let [ev (<! ch)]
          (set! (.-src img) (aget ev "target" "result"))
          (<! ch)
          (>! bg-image-chan img))))))

(defn- read!
  []
  (edn/read-string (.-value (u/dom "banner-declaration"))))

(generate! (read!) :no-image)
