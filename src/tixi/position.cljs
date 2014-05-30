(ns tixi.position
  (:require-macros [dommy.macros :refer (node sel1)]
                   [tixi.utils :refer (b)])
  (:require [dommy.core :as dommy]
            [tixi.geometry :as g :refer [Size Rect Point]]
            [tixi.data :as d]
            [tixi.utils :refer [p]]))

(defn- calculate-letter-size []
  (let [number-of-x 100]
    (dommy/append! (sel1 :body)
             [:.calculate-letter-size (apply str (repeat number-of-x "X"))])
    (let [calculator (sel1 :.calculate-letter-size)
          width (.-offsetWidth calculator)
          height (.-offsetHeight calculator)
          result (Size. (.round js/Math (/ width number-of-x)) height)]
      (dommy/remove! calculator)
      result)))

(defn- letter-size []
  ((memoize calculate-letter-size)))

(defn- hit-item? [item point]
  (let [{:keys [origin dimensions]} (:cache item)
        rect (g/build-rect origin dimensions)]
    (g/inside? rect point)))

(defn- item-has-point? [item point]
  (let [points (keys (:points (:cache item)))
        moved-point (g/sub point (:origin (:cache item)))]
    (not= (some #{moved-point} points) nil)))

(defn canvas-size []
  (Size. (.floor js/Math (/ (.-innerWidth js/window) (:width (letter-size))))
         (.floor js/Math (/ (.-innerHeight js/window) (:height (letter-size))))))

(defprotocol IConvert
  (position->coords [this])
  (coords->position [this]))

(extend-type Rect
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x1 y1 x2 y2] (g/values this)]
      (Rect. (Point. (.floor js/Math (/ x1 width))
                     (.floor js/Math (/ y1 height)))
             (Point. (.floor js/Math (/ x2 width))
                     (.floor js/Math (/ y2 height))))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
          [x1 y1 x2 y2] (g/values this)]
      (Rect. (Point. (.floor js/Math (* x1 width))
                     (.floor js/Math (* y1 height)))
             (Point. (.floor js/Math (* x2 width))
                     (.floor js/Math (* y2 height)))))))

(extend-type Point
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x y] (g/values this)]
      (Point. (.floor js/Math (/ x width))
              (.floor js/Math (/ y height)))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
         [x y] (g/values this)]
      (Point. (.floor js/Math (* x width))
              (.floor js/Math (* y height))))))

(extend-type Size
  IConvert
  (position->coords [this]
    (let [{:keys [width height]} (letter-size)
          [x y] (g/values this)]
      (Size. (.floor js/Math (/ x width))
             (.floor js/Math (/ y height)))))
  (coords->position [this]
    (let [{:keys [width height]} (letter-size)
         [x y] (g/values this)]
      (Size. (.floor js/Math (* x width))
             (.floor js/Math (* y height))))))

(defn event->coords [event]
  (let [root (or (sel1 :.canvas) (js-obj "offsetLeft" 0 "offsetTop" 0))]
    (let [x (- (.-clientX event) (.-offsetLeft root))
          y (- (.-clientY event) (.-offsetTop root))]
      (position->coords (Point. x y)))))

(defn items-inside-rect [rect]
  (filter (fn [[_ item]]
            (let [input-rect (g/normalize (:input item))
                  sel-rect (g/normalize rect)]
              (g/inside? sel-rect input-rect)))
          (d/completed)))

(defn items-at-point [point]
  (->> (d/completed)
       (filter (fn [[_ item]] (hit-item? item point)))
       (filter (fn [[_ item]] (item-has-point? item point)))))

(defn items-wrapping-rect [ids]
  (when (and ids (not-empty ids))
    (g/wrapping-rect (map #(:input (d/completed-item %)) ids))))
