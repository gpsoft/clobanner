(ns clobanner.core)

(defn- find-canvas
  [canvas-id img-id]
  (let [can (.getElementById js/document canvas-id)
        ctx (.getContext can "2d")
        img (.getElementById js/document img-id)]
    {:canvas can :context ctx :image img}))
(defonce the-canvas (find-canvas "work-canvas" "result-image"))

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

(defn- generate!
  [c mime]
  (let [can (:canvas c)
        img (:image c)
        url (.toDataURL can mime)]
    (set! (.-src img) url)))

(doto the-canvas
  (resize! 600 300)
  clear!
  (fill! "#6699cc")
  (text! 100 150 "Hey" "bold 80px 'monospace'" "#ff0000" "#ffffff" 3)
  (text! 150 250 "よろしくお願いします。" "bold 40px 'serif'" "#ffffff")
  (generate! "image/jpeg"))

