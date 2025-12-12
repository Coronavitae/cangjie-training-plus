(ns cangjie-training.state)

;; Shared application state

(defonce *practice-mode? (atom false))
(defonce *show-pinyin? (atom false))