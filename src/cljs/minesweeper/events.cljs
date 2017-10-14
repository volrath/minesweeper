(ns minesweeper.events
  (:require [minesweeper.core :refer [clear-pos empty-field plant-mines]]
            [minesweeper.db :refer [default-db]]
            [re-frame.core :as rf]))

(rf/reg-event-db
 :db-initialize
 (fn [_ _]
   default-db))


(rf/reg-event-db
 :click-cell
 (fn [{:keys [db]} [_ clicked-cell]]
   (let [db (if (:started? db)
              db
              (-> db
                  (update :field plant-mines 40 clicked-cell)
                  (assoc :started? true)))]
     {:db (update db :field clear-pos clicked-cell)})))


(rf/reg-event-db
 :select-difficulty
 (fn [{:keys [db]} [_ {:keys [rows cols] :as difficulty}]]
   {:db (-> db
            (assoc :field (empty-field rows cols))
            (assoc :status :pending)
            (assoc :difficulty difficulty))}))
