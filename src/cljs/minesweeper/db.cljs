(ns minesweeper.db
  (:require [cljs.spec.alpha :as s]))

;; State Machine
(def game-fsm {'SelectDifficulty  {:set-difficulty  'Ready}
               'Ready             {:reset           'SelectDifficulty
                                   :start           'Running}
               'Running           {:reset           'SelectDifficulty
                                   :pause           'Paused
                                   :win             'WonGame
                                   :lose            'LostGame}
               'Paused            {:resume          'Running}
               'WonGame           {:reset           'SelectDifficulty}
               'LostGame          {:reset           'SelectDifficulty}})
(s/def ::status (set (keys game-fsm)))

;; Timer
(s/def ::elapsed-time (s/and int? #(>= % 0)))

;; Difficulty
(s/def ::rows pos-int?)
(s/def ::cols pos-int?)
(s/def ::mines pos-int?)
(s/def ::difficulty (s/keys :req-un [::rows ::cols ::mines]))

;; Quadrant in field
(s/def ::mined? boolean?)
(s/def ::state #{:unknown :cleared :flagged})
(s/def ::adjacent-mines (s/and int? #(>= % 0)))
(s/def ::quadrant (s/keys :req-un [::mined? ::state] :opt-un [::adjacent-mines]))

;; Field: is just a vector of vectors of quadrants.
(s/def ::field-column (s/coll-of ::quadrant :kind vector?))
(s/def ::field (s/coll-of ::field-column :kind vector?))

;; DB
(s/def ::db (s/keys :req-un [::field ::elapsed-time ::status ::difficulty]))

(def default-db {:field        []
                 :elapsed-time 0
                 :status       'SelectDifficulty
                 :difficulty   nil})
