(ns cangjie-training.event-fx
  (:require [cangjie-training.dictionary :as cj-dict]
            [cangjie-training.learner :as learner]
            [cangjie-training.languages :as langs] ; Add languages for prompt text
            [cangjie-training.model :as model :refer [*learner-db]]
            [cangjie-training.state :as state] ; Use shared state
            [cangjie-training.util :refer [log]]
            [cljs.core.async :as async]
            [clojure.set :as set]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [rum.core :as rum]))


(def key-code->name
  {"KeyQ" "q" "KeyW" "w" "KeyE" "e" "KeyR" "r" "KeyT" "t" "KeyY" "y" "KeyU" "u"
   "KeyI" "i" "KeyO" "o" "KeyP" "p" "KeyA" "a" "KeyS" "s" "KeyD" "d" "KeyF" "f"
   "KeyG" "g" "KeyH" "h" "KeyJ" "j" "KeyK" "k" "KeyL" "l" "KeyZ" "z" "KeyX" "x"
   "KeyC" "c" "KeyV" "v" "KeyB" "b" "KeyN" "n" "KeyM" "m"
   "Tab" "Tab" "Backspace" "Backspace" "BracketLeft" "BracketLeft"
   "BracketRight" "BracketRight" "Space" "Space" "Slash" "Slash"
   "Backslash" "Backslash"})

(defn- key-event->msg [model key-code]
  (cond
    (= key-code "Space") [:msg/checked-answer]
    (= key-code "Backspace") [:msg/delete-last-char]
    (= key-code "Tab") [:msg/show-hint]
    (= key-code "BracketRight") [:msg/viz-next-page]
    (= key-code "BracketLeft") [:msg/viz-prev-page]
    (= key-code "Slash") [:msg/set-stats-show-hide (not (:show-stats? model))]
    (= key-code "Backslash") [:msg/toggle-practice-mode]
    (= key-code "Digit0") [:msg/toggle-pinyin]
    (= key-code "Digit1") [:msg/expand-learner-pool model/learn-more-word-count
                           (langs/text :cangjie-training.ui/label--learn-more-prompt ::langs/display-lang--english)]
    (= key-code "Digit2") [:msg/continue-review]
    (= key-code "Digit3") [:msg/review-hardest]
    :else (let [key-name (key-code->name key-code)]
            (if (contains? model/keyboard-key->cj-part key-name)
              [:msg/enter-char key-name]
              []))))

(defn key-event [model key-name]
  (key-event->msg model (get (set/map-invert key-code->name) key-name)))

(defn- update-model
  "update app model provided keyboard input event 
   (update : Msg -> Model -> [Model Effect])"
  [model [message-type & msg-args]]
  (let [{:keys [question-char ans-parts hint-count parts-score answered?
                show-stats? viz-page]} model
        radicals (model/split-radicals question-char)]
    (case message-type

      ;; generate new question: ask user to answer next card
      :msg/new-question
      [(let [[learner-db] msg-args] (model/next-question model learner-db))
       nil]

      ;; question answered: update learner DB, then generate new question
      :msg/checked-answer
      (if answered?
        [model
         {:fx-type :fx/update-learner-db
          :post-fx (fn [learner-db] [[:msg/save-learner-db learner-db]
                                     [:msg/new-question learner-db]])}]
        [model nil])

      ;; delete last entered char
      :msg/delete-last-char
      (if (not answered?)
        [(assoc model
                :ans-parts  (-> ans-parts butlast vec))
         nil]
        [model nil])

      ;; add entered char
      :msg/enter-char
      (if (< (count ans-parts) (count radicals))
        (let [[char-input]   msg-args
              ans-part-index (count ans-parts)
              correct?       (= char-input (nth radicals ans-part-index))
              new-ans-parts  (conj ans-parts char-input)]
          [(assoc model
                  :ans-parts   new-ans-parts
                  :parts-score (model/score-part parts-score ans-part-index
                                                 correct?)
                  :answered?   (= new-ans-parts radicals))
           nil])
        [model nil])

      ;; show hint for next char
      :msg/show-hint
      (if (and (not= ans-parts radicals)
               (< hint-count (count radicals)))
        [(assoc model
                :hint-count  (min (inc hint-count) (count radicals))
                :parts-score (model/score-part-hint parts-score hint-count))
         nil]
        [model nil])

      ;; save learner DB somewhere
      :msg/save-learner-db
      [model
       {:fx-type :fx/persist-learner-db
        :learner-db (let [[learner-db] msg-args] learner-db)}]

      ;; pagination: next page
      :msg/viz-next-page
      (if show-stats?
        [(assoc model :viz-page (if (model/has-next-page? model @model/*learner-db)
                                  (inc viz-page)
                                  viz-page))
         nil]
        [model nil])

      ;; pagination: previous page
      :msg/viz-prev-page
      (if show-stats?
        [(assoc model :viz-page (if (> viz-page 1) (dec viz-page) viz-page))
         nil]
        [model nil])

      :msg/expand-learner-pool
      [model
       (let [[expand-count prompt-message] msg-args]
         {:fx-type :fx/expand-learner-pool
          :expand-count expand-count
          :prompt-message prompt-message
          :post-fx (fn [learner-db] [[:msg/save-learner-db learner-db]
                                     [:msg/new-question learner-db]])})]

      :msg/set-stats-show-hide
      [(assoc model :show-stats? (first msg-args))
       nil]

      :msg/continue-review
      [model
       {:fx-type :fx/continue-review
        :post-fx (fn [learner-db] [[:msg/save-learner-db learner-db]
                                     [:msg/new-question learner-db]])}]

      :msg/review-hardest
      [model
       {:fx-type :fx/review-hardest
        :post-fx (fn [learner-db] [[:msg/save-learner-db learner-db]
                                     [:msg/new-question learner-db]])}]

      :msg/toggle-practice-mode
      (let [new-practice-mode? (not (:practice-mode? model))
            new-hint-count (if new-practice-mode?
                            (count (model/split-radicals (:question-char model)))
                            (:hint-count model))]
        [(do
           (swap! state/*practice-mode? not)
           (assoc model 
                 :practice-mode? new-practice-mode?
                 :hint-count new-hint-count))
         nil])

      :msg/toggle-pinyin
      (let [new-show-pinyin? (not (:show-pinyin? model))]
        [(do
           (swap! state/*show-pinyin? not)
           (assoc model :show-pinyin? new-show-pinyin?))
         nil])

      [model nil])))


(defmulti do-effect! (fn [_model {:keys [fx-type]}] fx-type))

(defmethod do-effect! :fx/update-learner-db
  [model {:keys [post-fx]}]
  (let [{:keys [question-char parts-score question-start-time practice-mode?]} model
        grade (learner/grade-answer (get @*learner-db question-char)
                                    parts-score)
        answer-time-taken-ms (- (js/Date.now) question-start-time)
        ; Calculate pass/fail based on grade (0.8 threshold like in SM-2-mod)
        passed? (> grade 0.8)
        ; Calculate score incorporating both accuracy and speed
        total-parts (count parts-score)
        correct-parts (count (filter pos? parts-score))
        accuracy-score (if (pos? total-parts) (/ correct-parts total-parts) 0)
        ; Convert milliseconds to seconds for the time bonus calculation
        time-in-seconds (/ answer-time-taken-ms 1000.0)
        time-bonus (max 0 (* (- 4 time-in-seconds) 0.1))
        score (+ (* accuracy-score 0.7) time-bonus)]
        
    ; DEBUG: Log all calculation values
    (js/console.log "DEBUG: Scoring calculations:")
    (js/console.log "  answer-time-taken-ms:" answer-time-taken-ms)
    (js/console.log "  time-in-seconds:" time-in-seconds)
    (js/console.log "  total-parts:" total-parts)
    (js/console.log "  correct-parts:" correct-parts)
    (js/console.log "  accuracy-score:" accuracy-score)
    (js/console.log "  time-bonus:" time-bonus)
    (js/console.log "  final score:" score)
    
    ; Update daily statistics (only in normal mode, not practice mode)
    (when (not practice-mode?)
      (model/update-daily-stats! passed? score question-char))
    
    (if practice-mode?
      ; In practice mode: update review timing but preserve difficulty
      (let [current-stat (get @*learner-db question-char)
            ; Create updated stat with new review timing but same difficulty
            updated-stat (assoc current-stat
                              :dlr (js/Date.now)
                              :po 1)] ; po = 1 indicates a review occurred
        (post-fx (swap! *learner-db assoc question-char updated-stat)))
      ; In normal mode: update full statistics including difficulty
      (post-fx (swap! *learner-db update question-char
                      learner/update-stat grade (/ answer-time-taken-ms 1000))))))

(defmethod do-effect! :fx/start-new-session
  [_db {:keys [post-fx]}]
  (model/start-new-session!)
  (when post-fx (post-fx @model/*learner-db)))

(defmethod do-effect! :fx/persist-learner-db
  [_db {:keys [learner-db]}]
  (log "saving..." (clj->js learner-db))
  (model/persist-all-data! @model/*learner-db @model/*daily-stats))

(defmethod do-effect! :fx/expand-learner-pool
  [_db {:keys [expand-count prompt-message post-fx]}]
  (if-let [new-chars (seq (model/new-chars-to-learn expand-count
                                                    @model/*learner-db))]
    (when (js/confirm
           (gstring/format prompt-message expand-count (str/join " " new-chars)))
      (log "adding" expand-count "items to learner db pool" (str new-chars))
      (post-fx (swap! *learner-db merge (model/init-learner-db new-chars))))
    (js/alert "You have learnt all " (count cj-dict/popular-chinese-chars)
              " Chinese characters available in this app! 🎉")))

(defmethod do-effect! :fx/continue-review
  [_db {:keys [post-fx]}]
  ; Start new session when user continues review
  (model/start-new-session!)
  (let [chars-to-review (model/chars-for-continue-review @model/*learner-db)]
    (if (seq chars-to-review)
      (do
        (log "Continuing review - chars to review:" chars-to-review)
        (let [updated-db (swap! *learner-db model/set-chars-due-now chars-to-review)]
          (log "Updated DB:" (count updated-db) "chars, due chars:" (count (model/items-to-review updated-db)))
          (post-fx updated-db)))
      (js/alert "No characters available for review yet. Keep learning!"))))

(defmethod do-effect! :fx/review-hardest
  [_db {:keys [post-fx]}]
  ; Start new session when user reviews hardest
  (model/start-new-session!)
  (let [chars-to-review (model/chars-for-hardest-review @model/*learner-db)]
    (if (seq chars-to-review)
      (do
        (log "Reviewing hardest - chars to review:" chars-to-review)
        (let [updated-db (swap! *learner-db model/set-chars-due-now chars-to-review)]
          (log "Updated DB:" (count updated-db) "chars, due chars:" (count (model/items-to-review updated-db)))
          (post-fx updated-db)))
      (js/alert "No characters available for hardest review yet. Keep learning!"))))

(defn init-event-msg-chan [*model *pressed-keys]
  (let [>message-chan (async/chan)]
    ;; app event loop
    (async/go-loop []
      (->> (async/<! >message-chan)
           (swap! *model (fn [model message]
                           (let [[next-model effect] (update-model model message)]
                             (when effect (doseq [msg (do-effect! model effect)]
                                            (async/put! >message-chan msg)))
                             next-model))))
      (recur))
    ;; hook keyboard event
    (let [on-key-up
          (fn [event]
            (async/put! >message-chan (key-event->msg @*model (.-code event)))
            (swap! *pressed-keys disj (.-code event))
            (.preventDefault event)
            false)
          on-key-down
          (fn [event]
            ; disable Tab key moving keyboard focus away from page
            (when (#{"Tab" "/"} (.-key event)) (.preventDefault event))
            ;; (log (.-code event))
            (swap! *pressed-keys conj (.-code event)))]
      (rum/use-effect! ; react hook
       (fn []
         (.addEventListener js/document "keyup" on-key-up)
         (.addEventListener js/document "keydown" on-key-down)
         (fn []
           (.removeEventListener js/document "keyup" on-key-up)
           (.removeEventListener js/document "keydown" on-key-down)))
       []))
    ;; return async channel
    >message-chan))
