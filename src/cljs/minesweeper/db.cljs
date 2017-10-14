(ns minesweeper.db
  (:require [cljs.spec.alpha :as s]))

;; Status
(s/def ::status #{:select-difficulty :pending :running :paused :win :lost})

;; Difficulty
(s/def ::rows pos-int?)
(s/def ::cols pos-int?)
(s/def ::mines pos-int?)
(s/def ::difficulty (s/keys :req-un [::rows ::cols ::mines]))

;; Quadrant in field
(s/def ::mined? boolean?)
(s/def ::cleared? boolean?)

(s/def ::quadrant (s/keys :req-un [::mined? ::cleared?]))

;; Field: is just a vector of vectors of quadrants.
(s/def ::field-column (s/coll-of ::quadrant :kind vector?))
(s/def ::field (s/coll-of ::field-column :kind vector?))

;; DB
(s/def ::db (s/keys :req-un [::field ::status ::difficulty]))

(def default-db {:field      []
                 :status     :select-difficulty
                 :difficulty nil})
