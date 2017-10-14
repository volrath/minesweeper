(ns ^{:author "Daniel Barreto"
      :doc "Main game logic."}
    minesweeper.core)


(defn mined? [field x y]
  (get-in field [x y :mined?]))


(defn empty-field
  "Return a mine field: grid of `rows`x`cols` positions, where every position has
  no bomb and hasn't been cleared yet.

  The mine field is just a matrix of position data structures. Positions are
  defined as:

  {:mined? <bool>
   :cleared? <bool>}

  A 'cleared mined' position means the user lost the game."
  [rows cols]
  (let [positions (repeat {:mined?   false
                           :cleared? false})]
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
      (let [rows (count field)
            cols (count (first field))
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


(defn clear-pos [field clicked-cell]
  )
