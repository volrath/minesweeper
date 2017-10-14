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
