(ns minesweeper.views
  (:require [cljss.reagent :refer-macros [defstyled]]
            [re-frame.core :as rf]))

;; Actual Game UI
;; -----------------------------------------------------------------------------

(defstyled grid :div
  {:display "grid"
   :height "500px"
   :justify-items "stretch"
   :align-items "stretch"
   :justify-content "center"})


(defn cell [x y {:keys [cleared? mined?]}]
  ^{:key (str "c-" x "-" y)}
  [:div {:on-click #(rf/dispatch [:click-cell {:x x :y y}])
         :style {:grid-row       (str (+ x 1) " / " (+ x 2))
                 :grid-column    (str (+ y 1) " / " (+ y 2))
                 :background     (cond (not cleared?) "#ddd"
                                       mined?         "#444"
                                       :else          "#e5e5e5")
                 :color          (case cleared?
                                   1 "blue"
                                   2 "green"
                                   3 "red"
                                   4 "purple"
                                   5 "brown"
                                   6 "pink"
                                   7 "yellow"
                                   8 "black"
                                   nil)
                 :border         "1px solid #bbb"
                 :cursor         "pointer"
                 :text-align     "center"
                 :vertical-align "center"}}
   (when (and cleared? (pos? cleared?))
     cleared?)])


(defn field-grid [rows cols]
  [grid {:style {:grid-template-columns (str "repeat(" cols ", 1fr)")
                 :grid-template-rows    (str "repeat(" rows ", 1fr)")}}
   (let [field @(rf/subscribe [:field])]
     (doall
      (for [x (range rows)]
        (doall
         (for [y (range cols)]
           (cell x y (get-in field [x y])))))))])


;; Difficulty Selection
;; -----------------------------------------------------------------------------

(defn select-difficulty []
  [:div "Select your difficulty..."])


;; App Level Interface
;; -----------------------------------------------------------------------------

(defn minesweeper []
  (if-let [{:keys [rows cols]} @(rf/subscribe [:difficulty])]
    (field-grid rows cols)
    (select-difficulty)))
