(ns minesweeper.events
  (:require [minesweeper.core :refer [clear-quadrant clear-quadrant-expansion
                                      empty-field plant-mines toggle-quadrant-mark]]
            [minesweeper.db :refer [default-db]]
            [re-frame.core :as rf]))

(rf/reg-event-db
 :db-initialize
 (fn [_ _]
   default-db))


(rf/reg-event-db
 :click-cell
 (fn [{:keys [db]} [_ clicked-cell cleared? flagged? ctrl?]]
   (let [mines (get-in db [:difficulty :mines])
         db (if (= (:status db) :pending)
              (-> db
                  (update :field plant-mines mines clicked-cell)
                  (assoc :status :running))
              db)]
     {:db
      (if (= (:status db) :running)  ;; If not running, there's no point...
        ;; First we determine the type of action the user is trying to take over
        ;; the clicked cell
        (let [noop   (fn [x _] x)
              action (cond ctrl?    toggle-quadrant-mark
                           flagged? noop
                           cleared? clear-quadrant-expansion
                           :else    clear-quadrant)]
          ;; Then we change the world...
          (update db :field action clicked-cell))
        db)})))


(rf/reg-event-db
 :select-difficulty
 (fn [{:keys [db]} [_ {:keys [rows cols] :as difficulty}]]
   {:db (-> db
            (assoc :field (empty-field rows cols))
            (assoc :status :pending)
            (assoc :difficulty difficulty))}))
