(ns ^{:author "Daniel Barreto"
      :doc "Main game logic."}
    minesweeper.core
  (:require [re-frame.core :as rf]))


(defn unknown? [field x y]
  (= (get-in field [x y :state]) :unknown))

(defn cleared? [field x y]
  (= (get-in field [x y :state]) :cleared))


(defn flagged? [field x y]
  (= (get-in field [x y :state]) :flagged))


(defn mined? [field x y]
  (get-in field [x y :mined?]))


(defn rows [field]
  (count field))


(defn cols [field]
  (count (first field)))


(defn empty-field
  "Return a mine field: grid of `rows`x`cols` positions, where every position has
  no bomb and hasn't been cleared yet.

  The mine field is just a matrix of position data structures. Positions are
  defined as:

  {:mined? <bool>
   :state  <:unknown|:cleared|:flagged>
   :adjacent-mines <optional int>}

  A 'cleared mined' position means the user lost the game."
  [rows cols]
  (let [positions (repeat {:mined? false
                           :state :unknown})]
    (into [] (take rows (repeat
                         (into [] (take cols positions)))))))


(defn adjacent-coordinates
  "Return a list of all adjacent coordinates to the given `x`, `y` values, in the
  given `rows`x`cols` grid.  This function also includes `x` and `y` into the
  returned list."
  [{:keys [x y] :as c} rows cols]
  (loop [neighbors          (list c)
         possible-neighbors (list [-1 -1]  ;; TODO: pretty sure there's a better way to generate this.
                                  [0 -1]
                                  [1 -1]
                                  [-1 0]
                                  [0 0]
                                  [1 0]
                                  [-1 1]
                                  [0 1]
                                  [1 1])]
    (if (empty? possible-neighbors)
      neighbors
      (let [[cx cy] (first possible-neighbors)
            nx      (+ x cx)
            ny      (+ y cy)]
        (if (and (<= 0 nx (dec rows))
                 (<= 0 ny (dec cols)))
          (recur (cons {:x nx :y ny} neighbors)
                 (rest possible-neighbors))
          (recur neighbors
                 (rest possible-neighbors)))))))


(defn plant-mines
  "Places an amount of `mines` in `field`, avoiding `init-click` coordinates and
  all its possible adjacent positions."
  [field mines init-click]
  (loop [mine-nr 0
         field field]
    (if (>= mine-nr mines)
      field
      (let [rows (rows field)
            cols (cols field)
            mine-x (rand-int rows)
            mine-y (rand-int cols)
            same-position (fn [{:keys [x y]}]
                            (and (= x mine-x)
                                 (= y mine-y)))]
        (if (or (mined? field mine-x mine-y)
                (some same-position (adjacent-coordinates init-click rows cols)))
          (recur mine-nr field)  ;; If it's already mined or it's close to the initial click, try again.
          (recur (inc mine-nr)
                 (assoc-in field [mine-x mine-y :mined?] true)))))))


(defn uncover-mines
  "Return a `field` where all the mined quadrants are `:cleared`."
  [field]
  (mapv (fn [row]
          (mapv (fn [q]
                  (if (and (:mined? q)
                           (not= (:state q) :flagged))
                    (assoc q :state :cleared)
                    q))
                row))
        field))


(defn game-end-condition
  "Check `field` to see if the user won or lost the game.

  Returns a FMS transition `:win` `:lose` or `nil` accordingly."
  [field]
  (loop [field field
         won   true
         mined false]
    (let [row (first field)]
      (if (and row (not mined))
        (recur (rest field)
               (and won
                    (every? #(let [state (:state %)]  ;; All quadrants are either cleared or correctly flagged
                               (or (= state :cleared)
                                   (and (= state :flagged)
                                        (:mined? %))))
                            row))
               (not (not-any? #(and (:mined? %)  ;; wtf cljs? why not `any?`
                                    (= (:state %) :cleared))
                              row)))
        (cond mined :lose
              won   :win
              :else nil)))))


;; Users update the world through these functions

(defn clear-quadrant
  "Runs when a user clicks on a unknown quadrant at `x`,`y`.

  Receives the current `field` and returns an updated `field` with the clear
  quadrant(s) updated.  If the `x`,`y` position is a \"zero quadrant\" (no mines
  surrounding it), recursively clear neighbors."
  [field {:keys [x y] :as q}]
  (if (unknown? field x y)  ;; Only need to pay attention to unknown quadrants
    (let [neighbors      (adjacent-coordinates q (rows field) (cols field))
          adjacent-mines (count (filter (fn [{:keys [x y]}] (mined? field x y))
                                        neighbors))
          field          (-> field
                             (assoc-in [x y :state] :cleared)
                             (assoc-in [x y :adjacent-mines] adjacent-mines))]
      (if (pos? adjacent-mines)
        field
        (reduce clear-quadrant field neighbors)))
    field))


(defn clear-quadrant-expansion
  "Runs when a user clicks on a cleared quadrant at `x`,`y`.

  Receives the current `field` and returns an updated `field` with a possible
  expansion over the clicked cleared quadrant."
  [field {:keys [x y] :as q}]
  (if (or (unknown? field x y)
          (flagged? field x y))
    field
    (let [neighbors       (adjacent-coordinates q (rows field) (cols field))
          filter-adjacent (fn [pred] (filter (fn [{:keys [x y]}] (pred field x y)) neighbors))
          adjacent-mines  (count (filter-adjacent mined?))
          adjacent-flags  (count (filter-adjacent flagged?))]
      (if (= adjacent-flags adjacent-mines)
        (reduce clear-quadrant field (filter-adjacent unknown?))
        field))))


(defn toggle-quadrant-mark
  "Toggles the state of a quadrant at `x`,`y`."
  [field {:keys [x y]}]
  (if (cleared? field x y)
    field
    (update-in field [x y :state] #(if (= % :unknown) :flagged :unknown))))
