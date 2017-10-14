(ns minesweeper.app
  (:require minesweeper.events  ;; Only here to make sure it's loaded
            minesweeper.subs    ;; by the compiler
            [minesweeper.views :refer [minesweeper]]
            [re-frame.core :as rf]
            [re-frisk.core :refer [enable-re-frisk!]]
            [reagent.core :as reagent]))

(enable-re-frisk!)

(defn init []
  (rf/dispatch-sync [:db-initialize])
  (rf/dispatch-sync [:select-difficulty {:rows  16
                                         :cols  16
                                         :mines 40}])
  (reagent/render-component
   [minesweeper]
   (.getElementById js/document "container")))
