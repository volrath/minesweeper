(ns minesweeper.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :field
 (fn [{:keys [db]} _]
   (:field db)))


(rf/reg-sub
 :status
 (fn [{:keys [db]} _]
   (:status db)))


(rf/reg-sub
 :difficulty
 (fn [{:keys [db]} _]
   (:difficulty db)))

(rf/reg-sub
 :quadrant
 (fn [query-v _]
   (rf/subscribe [:field]))
 (fn [field query-v _]
   (let [[x y] (rest query-v)]
     (get-in field [x y]))))


(rf/reg-sub
 :elapsed-time
 (fn [{:keys [db]} _]
   (:elapsed-time db)))

(rf/reg-sub
 :timer
 :<- [:elapsed-time]
 (fn [elapsed-ms query-v _]
   (let [seconds  (js/Math.floor (/ elapsed-ms 1000))
         minutes  (js/Math.floor (/ seconds 60))
         left-pad #(str (if (< % 10) "0" "") %)]
     (str (left-pad minutes) ":" (left-pad (mod seconds 60))))))
