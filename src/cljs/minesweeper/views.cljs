(ns minesweeper.views
  (:require [cljss.reagent :refer-macros [defstyled]]
            [re-frame.core :as rf]))

;; Timer / Status

(defn game-status [status]
  (let [timer  @(rf/subscribe [:timer])]
    [:div {:style {:font-size "60px"}}
     [:span timer]
     [:span (case status
              Ready "🙉"
              Running "🐵"
              Paused "🙈"
              WonGame "🎉"
              LostGame "🙊")]]))


;; Field Grid
;; -----------------------------------------------------------------------------

(defstyled grid :div
  {:display "grid"
   :height "500px"
   :justify-items "stretch"
   :align-items "stretch"
   :justify-content "center"})


(defn handle-click-cell [x y cell-state game-status]
  (fn [ev]
    (when (or (= game-status 'Running)
              (= game-status 'Ready))
      (let [click {:x x :y y}]
        (when (= game-status 'Ready)
          (rf/dispatch [:change-status :start click]))
        (rf/dispatch [:click-cell click cell-state (.-ctrlKey ev)])))))

(defn cell [x y {:keys [state mined? adjacent-mines]} game-status]
  ^{:key (str "c-" x "-" y)}
  [:div {:on-click (handle-click-cell x y state game-status)
         :style    {:grid-row       (str (+ x 1) " / " (+ x 2))
                    :grid-column    (str (+ y 1) " / " (+ y 2))
                    :background     (cond (= state :flagged) "#ff8888"
                                          (= state :unknown) "#ddd"
                                          mined?             "#444"
                                          (= state :cleared) "#f0f0f0")
                    :color          (case adjacent-mines
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
   (when (and adjacent-mines (pos? adjacent-mines))
     adjacent-mines)])


(defn field-grid [status rows cols]
  [grid {:style {:grid-template-columns (str "repeat(" cols ", 1fr)")
                 :grid-template-rows    (str "repeat(" rows ", 1fr)")}}
   (let [field @(rf/subscribe [:field])]
     (doall
      (for [x (range rows)]
        (doall
         (for [y (range cols)]
           (cell x y (get-in field [x y]) status))))))])


;; Difficulty Selection
;; -----------------------------------------------------------------------------

(defn select-difficulty []
  [:div "Select your difficulty..."])


;; App Level Interface
;; -----------------------------------------------------------------------------

(defn game [status]
  (let [{:keys [rows cols]} @(rf/subscribe [:difficulty])]
    [:div
     (game-status status)
     (field-grid status rows cols)]))

(defn minesweeper []
  (let [status @(rf/subscribe [:status])]
    (if (= status 'SelectDifficulty)
      (select-difficulty)
      (game status))))
