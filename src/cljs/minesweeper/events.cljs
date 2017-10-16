(ns minesweeper.events
  (:require [minesweeper.core :refer [clear-quadrant clear-quadrant-expansion
                                      empty-field plant-mines toggle-quadrant-mark]]
            [minesweeper.db :refer [default-db game-fsm]]
            [re-frame.core :as rf]))


(defn reset-db [{:keys [db]} _]
  {:db (merge db default-db)})


(rf/reg-event-db
 :db-initialize
 reset-db)


(rf/reg-event-db
 :click-cell
 (fn [{:keys [db]} [_ clicked-cell cell-state ctrl?]]
   {:db
    (if (= (:status db) 'Running)  ;; If not running, there's no point...
      ;; First we determine the type of action the user is trying to take over
      ;; the clicked cell
      (let [noop   (fn [x _] x)
            action (cond ctrl?                   toggle-quadrant-mark
                         (= cell-state :flagged) noop
                         (= cell-state :cleared) clear-quadrant-expansion
                         :else                   clear-quadrant)]
        ;; Then we change the world...
        (update db :field action clicked-cell))
      db)}))


(rf/reg-event-db
 :update-time
 (fn [{:keys [db]} [_ elapsed]]
   {:db (update db :elapsed-time + elapsed)}))


;; Status hooks

(defn set-difficulty [{:keys [db]} [difficulty]]
  {:db (assoc db :difficulty difficulty)})


(defn create-empty-field [{:keys [db]} [{:keys [rows cols]}]]
  {:db (assoc db :field (empty-field rows cols))})


(defn plant-initial-mines [{:keys [db]} [initial-cell]]
  (let [mines (get-in db [:difficulty :mines])]
    {:db (update db :field plant-mines mines initial-cell)}))


(defn turn-on-timer [{:keys [db]} _]
  (let [i (/ 1000.0 12)]
    {:db (assoc db :js-interval (js/setInterval #(rf/dispatch [:update-time i]) i))}))

(defn turn-off-timer [{:keys [db]} _]
  {:db (assoc db :js-interval (js/clearInterval (:js-interval db)))})


;; Status changes

(def status-hooks
  {'SelectDifficulty {:in  [reset-db]
                      :out [set-difficulty]}
   'Ready            {:in  [create-empty-field]
                      :out [plant-initial-mines]}
   'Running          {:in  [turn-on-timer]
                      :out [turn-off-timer]}
   })


(defn run-hooks [pipeline status db args]
  (when-let [hooks (get-in status-hooks [status pipeline])]
    (loop [ctx      {:hooks hooks :db db}]
      (if-let [hook (first (:hooks ctx))]
        (let [next-ctx (update ctx :hooks rest)]
          (recur (merge next-ctx (hook next-ctx args))))
        (:db ctx)))))


(rf/reg-event-db
 :change-status
 (fn [{:keys [db]} [_ transition & args]]
   (let [prev-status (:status db)
         next-status (get-in game-fsm [(:status db) transition])
         merge-hooks (fn [db pipeline state]
                       (merge db (run-hooks pipeline state db args)))]
     {:db (-> db
              (merge-hooks :out prev-status)
              (assoc :status next-status)
              (merge-hooks :in next-status))})))
