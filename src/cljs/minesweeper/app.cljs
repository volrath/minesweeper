(ns minesweeper.app
  (:require minesweeper.events  ;; Only here to make sure it's loaded
            minesweeper.subs    ;; by the compiler
            [minesweeper.views :refer [container minesweeper]]
            [re-frame.core :as rf]
            [re-frisk.core :refer [enable-re-frisk!]]
            [reagent.core :as reagent]))

(enable-re-frisk!)

(defn init []
  (rf/dispatch-sync [:db-initialize])
  (reagent/render-component
   [minesweeper]
   container))
