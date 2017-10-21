(ns minesweeper.events
  (:require [minesweeper.core :refer [clear-quadrant clear-quadrant-expansion
                                      empty-field game-end-condition plant-mines
                                      toggle-quadrant-mark uncover-mines]]
            [minesweeper.db :refer [default-db game-fsm]]
            [re-frame.core :as rf]))

(def timer-manager
  (rf/->interceptor
   :id :timer-manager
   :after (fn [context]
            (if-let [[action timer-id] (or (rf/get-effect context :timer)
                                           (rf/get-coeffect context :timer))]
              (let [action-fn (case action
                                :stop  (fn [timer-id]
                                         (when timer-id
                                           (js/clearInterval timer-id)))
                                :start (fn [fps]
                                         (js/setInterval #(rf/dispatch [:update-time fps]) fps)))]
                (-> context
                    (assoc-in [:effects :db :js-interval] (action-fn timer-id))
                    (update :effects #(dissoc % :timer))))
              context))))


(defn reset-game [{:keys [db]} _]
  (let [timer-id (:js-interval db)]
    {:db (merge db default-db)
     :timer [:stop timer-id]}))


(rf/reg-event-fx
 :db-initialize
 [timer-manager]
 reset-game)


(rf/reg-event-db
 :click-cell
 (fn [db [_ clicked-cell cell-state ctrl?]]
   (if (= (:status db) 'Running)  ;; If not running, there's no point...
     ;; First we determine the type of action the user is trying to take over
     ;; the clicked cell
     (let [noop   (fn [x _] x)
           action (cond ctrl?                   toggle-quadrant-mark
                        (= cell-state :flagged) noop
                        (= cell-state :cleared) clear-quadrant-expansion
                        :else                   clear-quadrant)]
       ;; Then we change the world...
       (let [new-db (update db :field action clicked-cell)]
         (when-let [transition (game-end-condition (:field new-db))]
           (rf/dispatch [:change-status transition]))
         new-db))
     db)))


(rf/reg-event-db
 :update-time
 (fn [db [_ elapsed]]
   (update db :elapsed-time + elapsed)))


;; Status hooks

(defn set-difficulty [{:keys [db]} [difficulty]]
  {:db (assoc db :difficulty difficulty)})


(defn create-empty-field [{:keys [db]} [{:keys [rows cols]}]]
  {:db (assoc db :field (empty-field rows cols))})


(defn plant-initial-mines [{:keys [db]} [initial-cell]]
  (let [mines (get-in db [:difficulty :mines])]
    {:db (update db :field plant-mines mines initial-cell)}))


(defn turn-on-timer [{:keys [db]} _]
  {:timer [:start (/ 1000.0 12)]})

(defn turn-off-timer [{:keys [db]} _]
  {:timer [:stop (:js-interval db)]})


(defn uncover-mine-field [{:keys [db]} _]
  {:db (update db :field uncover-mines)})


;; Status changes

(def status-hooks
  {'SelectDifficulty {:entering [reset-game]
                      :leaving  [set-difficulty]}
   'Ready            {:entering [create-empty-field]
                      :leaving  [plant-initial-mines]}
   'Running          {:entering [turn-on-timer]
                      :leaving  [turn-off-timer]}
   'LostGame         {:entering [uncover-mine-field]}})


(defn next-status [db transition]
  (get-in game-fsm [(:status db) transition]))


(defn run-hooks [cofx hooks args]
  (loop [hooks hooks
         cofx  cofx]
    (if-let [hook (first hooks)]
      (recur (rest hooks)
             (merge cofx (hook cofx args)))
      cofx)))


(defn process-hook-map [hooks-map pipeline]
  (fn [context]
    (if (next-status (rf/get-coeffect context :db)
                     (second (rf/get-coeffect context :event)))
      (let [fx-key       (if (= pipeline :entering) :effects :coeffects)
            [_ _ & args] (rf/get-coeffect context :event)
            status       (get-in context [fx-key :db :status])
            hooks        (get-in hooks-map [status pipeline])]
        (update context fx-key run-hooks hooks args))
      context)))


(def status-update-hooks
  (rf/->interceptor
   :id :status-update-hooks
   :before (process-hook-map status-hooks :leaving)
   :after  (process-hook-map status-hooks :entering)))


(rf/reg-event-db
 :change-status
 [timer-manager status-update-hooks]
 (fn [db [_ transition & args]]
   (when-let [nxt (next-status db transition)]
     (assoc db :status nxt))))
