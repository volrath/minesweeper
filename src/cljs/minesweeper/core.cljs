(ns ^{:author "Daniel Barreto"
      :doc "Main game logic."}
    minesweeper.core)


(defn cleared? [field x y]
  (get-in field [x y :cleared?]))


(defn flagged? [field x y]
  (get-in field [x y :flagged?]))


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
   :cleared? <bool>}

  A 'cleared mined' position means the user lost the game."
  [rows cols]
  (let [positions (repeat {:mined?   false
                           :flagged? false
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


;; Users update the world through these functions

(defn clear-quadrant
  "Runs when a user clicks on a uncleared quadrant at `x`,`y`.

  Receives the current `field` and returns an updated `field` with the clear
  quadrant(s) updated.  If the `x`,`y` position is a \"zero quadrant\" (no mines
  surrounding it), recursively clear neighbors."
  [field {:keys [x y] :as q}]
  (if (cleared? field x y)  ;; If already cleared, nothing to do here.
    field
    (let [neighbors      (adjacent-coordinates q (rows field) (cols field))
          adjacent-mines (count (filter (fn [{:keys [x y]}] (mined? field x y))
                                        neighbors))
          field          (assoc-in field [x y :cleared?] adjacent-mines)]
      (if (pos? adjacent-mines)
        field
        (reduce clear-quadrant field neighbors)))))


(defn clear-quadrant-expansion
  "Runs when a user clicks on a cleared quadrant at `x`,`y`.

  Receives the current `field` and returns an updated `field` with a possible
  expansion over the clicked cleared quadrant."
  [field {:keys [x y] :as q}]
  (if (or (not (cleared? field x y))
          (flagged? field x y))
    field
    (let [neighbors       (adjacent-coordinates q (rows field) (cols field))
          filter-adjacent (fn [pred] (filter (fn [{:keys [x y]}] (pred field x y)) neighbors))
          adjacent-mines  (count (filter-adjacent mined?))
          adjacent-flags  (count (filter-adjacent flagged?))]
      (if (= adjacent-flags adjacent-mines)
        (reduce clear-quadrant field (filter-adjacent (comp not cleared?)))
        field))))


(defn toggle-quadrant-mark
  "Toggles the `flagged?` state of a quadrant at `x`,`y`."
  [field {:keys [x y]}]
  (if (cleared? field x y)
    field
    (update-in field [x y :flagged?] not)))
