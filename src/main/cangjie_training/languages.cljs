(ns cangjie-training.languages
  (:require [goog.string :as gstring]
            [goog.string.format]))


(def languages-text
  {::display-lang--chinese
   {:cangjie-training.ui/label--language "中 Chinese"
    :cangjie-training.ui/label--questions-remaining "剩餘 %s 字"
    :cangjie-training.ui/label--radical-hint "提示"
    :cangjie-training.ui/label--delete-radical "刪除"
    :cangjie-training.ui/label--next-word "下一個"
    :cangjie-training.ui/label--find-char-online "在www.hkcards.com查「%s」"
    :cangjie-training.ui/label--show-stats "顯示統計"
    :cangjie-training.ui/label--hide-stats "隱藏統計"
    :cangjie-training.ui/label--learning-words-out-of "正學習 %s 自 %s"
    :cangjie-training.ui/label--learning-words-popular "常用中文字"
    :cangjie-training.ui/label--stat-table--character "字"
    :cangjie-training.learner/mastery "精通"
    :cangjie-training.learner/last-review "上次重溫"
    :cangjie-training.learner/due "到期"
    :cangjie-training.learner/review-never "沒有"
    :cangjie-training.learner/due-now "現在"
    :cangjie-training.ui/label--learning-progress "進度"
    :cangjie-training.ui/label--learn-more-button "學習多%s個字"
    :cangjie-training.ui/label--continue-review-button "繼續學習"
    :cangjie-training.ui/label--review-hardest-button "複習最難"
    :cangjie-training.ui/label--toggle-practice-mode-button "切換練習模式"
    :cangjie-training.ui/label--toggle-pinyin-button "拼音"
    :cangjie-training.ui/label--practice-mode-indicator "練習模式 - 對精通無影響"
    :cangjie-training.ui/label--learn-more-prompt (str "將這%s個字添加到學習詞庫:\n"
                                                       "%s\n"
                                                       "你確定嗎?")
    :cangjie-training.ui/label--all-done-line-1 "重溫完畢。"
    :cangjie-training.ui/label--all-done-line-2 "請稍後回來！"
    :cangjie-training.util/label--recently "剛剛"
    :cangjie-training.util/label--soon "即將"
    :cangjie-training.ui/label--daily-stats-title "今日進度"
    :cangjie-training.ui/label--total-reviewed "已重溫字數"
    :cangjie-training.ui/label--total-passed "通過字數"
    :cangjie-training.ui/label--average-score "平均得分"}

   ::display-lang--english
   {:cangjie-training.ui/label--language "英 English"
    :cangjie-training.ui/label--questions-remaining "%s remaining"
    :cangjie-training.ui/label--radical-hint "Hint"
    :cangjie-training.ui/label--delete-radical "Delete"
    :cangjie-training.ui/label--next-word "Delete"
    :cangjie-training.ui/label--find-char-online "Find %s on www.hkcards.com"
    :cangjie-training.ui/label--show-stats "Show stats"
    :cangjie-training.ui/label--hide-stats "Hide stats"
    :cangjie-training.ui/label--learning-words-out-of "Learning %s of %s"
    :cangjie-training.ui/label--learning-words-popular "popular Chinese words"
    :cangjie-training.ui/label--stat-table--character "Character"
    :cangjie-training.learner/mastery "Mastery"
    :cangjie-training.learner/last-review "Last Review"
    :cangjie-training.learner/due "Due"
    :cangjie-training.learner/review-never "never"
    :cangjie-training.learner/due-now "now"
    :cangjie-training.ui/label--learning-progress "Progress"
    :cangjie-training.ui/label--learn-more-button "Learn %s more words"
    :cangjie-training.ui/label--continue-review-button "Continue Review"
    :cangjie-training.ui/label--review-hardest-button "Review Hardest"
    :cangjie-training.ui/label--toggle-practice-mode-button "Toggle Practice Mode"
    :cangjie-training.ui/label--toggle-pinyin-button "Pinyin"
    :cangjie-training.ui/label--practice-mode-indicator "Practice Mode - No Impact on Character Mastery"
    :cangjie-training.ui/label--learn-more-prompt
    (str "Add these %s characters to words pool:\n"
         "%s\n"
         "Are you sure?")
    :cangjie-training.ui/label--all-done-line-1 "Review Done."
    :cangjie-training.ui/label--all-done-line-2 "Come back later!"
    :cangjie-training.util/label--recently "recently"
    :cangjie-training.util/label--soon "soon"
    :cangjie-training.ui/label--daily-stats-title "Today's Progress"
    :cangjie-training.ui/label--total-reviewed "Characters Reviewed"
    :cangjie-training.ui/label--total-passed "Characters Passed"
    :cangjie-training.ui/label--average-score "Average Score"}})

(defn text [text-id language & args]
  (let [str-format (get-in languages-text [language text-id])]
    (if (some? str-format)
      (if (some? args)
        (apply gstring/format str-format args)
        str-format)
      (str text-id))) ; no translation fallback to text-id string
  )