(ns minesweeper.views
  (:require [cljss.reagent :refer-macros [defstyled]]
            [cljss.core :refer-macros [defstyles]]
            [re-frame.core :as rf]))

(def container (.getElementById js/document "minesweeper"))

;; Timer / Status

(defn game-status [status]
  (let [timer @(rf/subscribe [:timer])]
    [:div {:style {:font-size "60px"
                   :text-align "center"}}
     [:span timer]
     [:span {:style    {:cursor "pointer"}
             :on-click #(rf/dispatch [:change-status (case status
                                                       Paused   :resume
                                                       Running  :pause
                                                       WonGame  :reset
                                                       LostGame :reset)])}
      (case status
        Ready    "🙉"
        Running  "🐵"
        Paused   "🙈"
        WonGame  "🎉"
        LostGame "🙊")]]))


;; Field Grid
;; -----------------------------------------------------------------------------

(defstyled grid :div
  {:display "grid"
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
                    :background     (cond (= game-status 'Paused) "#c4c4c4"
                                          (= state :flagged) "#ddd"
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
                    :cursor         (if (or (= game-status 'Ready)
                                            (= game-status 'Running)) "pointer" "default")
                    :display "flex"
                    :align-items "center"
                    :justify-content "center"
                    :font-size "200%"
                    :user-select "none"
                    :-moz-user-select "none"}}
   (when (not= game-status 'Paused)
     (cond (= state :flagged) "🚩"
           (and (= state :cleared) mined?) "💥"
           (and adjacent-mines (pos? adjacent-mines)) adjacent-mines
           :else nil))])


(defn field-grid [status rows cols]
  (let [container-height (.-offsetHeight container)
        grid-size (/ container-height rows)]
    [grid {:style {:grid-template-columns (str "repeat(" cols ", " grid-size "px)")
                   :grid-template-rows    (str "repeat(" rows ", " grid-size "px)")}}
     (let [field @(rf/subscribe [:field])]
       (doall
        (for [x (range rows)]
          (doall
           (for [y (range cols)]
             (cell x y (get-in field [x y]) status))))))]))


;; Difficulty Selection
;; -----------------------------------------------------------------------------

(defstyles difficulty-option []
  {:padding "1em 0"
   :cursor  "pointer"})

(defn select-difficulty []
  (let [option-class (difficulty-option)
        option (fn [rows cols mines]
                 [:li {:class    option-class
                       :on-click #(rf/dispatch-sync [:change-status
                                                     :set-difficulty {:rows  rows
                                                                      :cols  cols
                                                                      :mines mines}])}
                  (str rows "x" cols " - " mines " mines")])]
    [:div
     [:h2 "Select Difficulty"]
     [:ul {:style {:list-style "none"
                   :color      "darkcyan"}}
      (option 16 16 40)
      (option 16 30 99)]]))


;; App Level Interface
;; -----------------------------------------------------------------------------

(defn game [status]
  (let [{:keys [rows cols]} @(rf/subscribe [:difficulty])]
    [:div
     (field-grid status rows cols)
     (game-status status)]))

(defn minesweeper []
  (let [status @(rf/subscribe [:status])]
    (if (= status 'SelectDifficulty)
      (select-difficulty)
      (game status))))
