(ns cangjie-training.model
  (:require [clojure.string :as str]
            [cangjie-training.dictionary :refer [popular-chinese-chars
                                                 radical-dict]]
            [cangjie-training.learner :as learner]
            [cangjie-training.util :refer [hours-diff log rescale]]))

;;; app model

(def init-learn-char-count 20)
(def learn-more-word-count 10)
(def viz-page-size-compact 3)
(def viz-page-size-long 10)

(def keyboard-key->cj-part
  {"q" "手", "w" "田", "e" "水", "r" "口", "t" "廿", "y" "卜", "u" "山", "i" "戈",
   "o" "人", "p" "心", "a" "日", "s" "尸", "d" "木", "f" "火", "g" "土", "h" "竹",
   "j" "十", "k" "大", "l" "中", "x" "難", "c" "金", "v" "女", "b" "月", "n" "弓",
   "m" "一"})

(defn split-radicals [question-char]
  (-> (radical-dict question-char) (str/split "")))

(defn persist-learner-db! [db-json]
  (->> db-json
       clj->js
       js/JSON.stringify
       (js/localStorage.setItem "cj-training-db-json")))

(defn persist-all-data! [learner-db daily-stats]
  "Save both learner DB and daily stats to localStorage"
  (let [combined-data (assoc learner-db "__daily_stats__" daily-stats)]
    (persist-learner-db! combined-data)))
#_(persist-learner-db! {"一" {:dbr 1 :d 0.3 :dlr (js/Date.now) :po 1}})

(defn load-learner-db! [map->learner-db-item]
  (let [db (-> (js/localStorage.getItem "cj-training-db-json")
               js/JSON.parse
               (js->clj :keywordize-keys true))]
    (reduce-kv (fn [m k v]
                 ; also convert string key instead of keyword key
                 (-> m (dissoc k) (assoc (name k) (map->learner-db-item v))))
               db db)))

(defn load-all-data! [map->learner-db-item]
  "Load both learner DB and daily stats from localStorage with backward compatibility"
  (let [data (-> (js/localStorage.getItem "cj-training-db-json")
                 js/JSON.parse
                 (js->clj :keywordize-keys true))
        
        ; Extract learner DB - filter out __daily_stats__ key and convert character keys
        learner-db (reduce-kv (fn [m k v]
                                (if (and (not= k "__daily_stats__") 
                                        (not= k :__daily_stats__)) ; Handle both string and keyword keys
                                  (-> m (dissoc k) (assoc (name k) (map->learner-db-item v)))
                                  m))
                              {} data)
        
        ; Extract daily stats with backward compatibility
        ; Try both string and keyword versions of the key
        daily-stats (or (get data "__daily_stats__")
                       (get data :__daily_stats__)
                       {:date (-> (js/Date.) .toDateString)
                        :current-session-id (str (js/Date.now))
                        :sessions {}
                        :total-reviewed 0
                        :total-passed 0
                        :total-scores []})]
    
    [learner-db daily-stats]))
#_(load-learner-db! identity)

(defn backup-db! [db-json]
  (->> db-json
       clj->js
       js/JSON.stringify
       (js/localStorage.setItem "cj-training-db-json-bak")))

(defn init-learner-db [items]
  (->> items
       (map (fn [char]
              ; difficulty based on number of radicals for the character
              (let [difficulty (rescale (count (split-radicals char))
                                        [1 5] [0.1 1])]
                [char #_(new-SM-2)
                 (learner/new-SM-2-mod difficulty)])))
       (into (hash-map))))
#_(cljs.pprint/pprint (init-learner-db
                       (take init-learn-char-count popular-chinese-chars)))

(defonce *learner-db-and-stats
  (let [[loaded-db loaded-stats] (load-all-data! learner/map->SM-2-mod)
        db (if (seq loaded-db)
             (do (backup-db! loaded-db) loaded-db)
             (init-learner-db (take init-learn-char-count
                                    popular-chinese-chars)))]
    [(atom db) (atom loaded-stats)]))

(defonce *learner-db (first *learner-db-and-stats))
(defonce *daily-stats (second *learner-db-and-stats))

;;; Daily statistics tracking with session awareness
(defonce *daily-stats (atom {:date (-> (js/Date.) .toDateString)
                             :current-session-id (str (js/Date.now)) ; Timestamp as session ID
                             :sessions {} ; {session-id {char1 true, char2 true}}
                             :total-reviewed 0
                             :total-passed 0
                             :total-scores []}))

(defn reset-daily-stats! []
  "Reset daily statistics if it's a new day"
  (let [today (-> (js/Date.) .toDateString)
        current-date (get @*daily-stats :date)]
    (when (not= today current-date)
      (reset! *daily-stats {:date today
                           :current-session-id (str (js/Date.now))
                           :sessions {}
                           :total-reviewed 0
                           :total-passed 0
                           :total-scores []}))))

(defn start-new-session! []
  "Start a new review session - call this when user completes reviews or presses Continue Review"
  (let [new-session-id (str (js/Date.now))]
    (swap! *daily-stats assoc
           :current-session-id new-session-id
           :sessions (assoc (:sessions @*daily-stats) new-session-id {}))))

(defn update-daily-stats! [passed? score question-char]
  "Update daily statistics with the latest review results, counting each character only once per session"
  (reset-daily-stats!)
  (let [session-id (:current-session-id @*daily-stats)
        session-chars (get (:sessions @*daily-stats) session-id {})
        already-counted? (contains? session-chars question-char)]
    
    (when (not already-counted?)
      ; Mark this character as counted in current session
      (swap! *daily-stats assoc-in [:sessions session-id question-char] true)
      
      ; Update statistics
      (swap! *daily-stats update :total-reviewed inc)
      (when passed? (swap! *daily-stats update :total-passed inc))
      (swap! *daily-stats update :total-scores conj score))))

(defn get-daily-stats []
  "Get current daily statistics"
  (reset-daily-stats!)
  @*daily-stats)

(defn calculate-average-score []
  "Calculate average score from daily statistics"
  (let [stats (get-daily-stats)
        scores (:total-scores stats)
        total (count scores)]
    (js/console.log "DEBUG: Average score calculation:")
    (js/console.log "  scores:" (clj->js scores))
    (js/console.log "  total scores:" total)
    (if (pos? total)
      (let [sum (apply + scores)
            avg (/ sum total)
            pct (* avg 100)
            result (js/Math.round pct)]
        (js/console.log "  sum:" sum)
        (js/console.log "  average:" avg)
        (js/console.log "  percentage:" pct)
        (js/console.log "  final result:" result)
        result)
      (do 
        (js/console.log "  No scores available, returning 0")
        0))))

(defn new-chars-to-learn [n learner-db]
  (->> popular-chinese-chars
       (filter #(not (contains? learner-db %)))
       (take n)))
#_(new-chars-to-learn 10)
#_(init-learner-db (new-chars-to-learn 10))
#_(merge @*learner-db (init-learner-db (new-chars-to-learn 10)))

(defn items-to-review [learner-db]
  (->> learner-db (filter (fn [[_char stat]] (learner/need-review? stat)))))

(defn review-in-next-hours [learner-db next-hours]
  (let [now (js/Date.now)]
    (->> learner-db vals
         (map #(hours-diff (learner/due-date %) now))
         (filter #(<= % next-hours)))))

(defn learner-progress [learner-db]
  (let [items (vals learner-db)
        total (count items)
        num-need-hint (->> items (filter learner/need-hint?) count)]
    (/ (- total num-need-hint) total)))
#_(learner-progress @*learner-db)

(defn learnt-chars [learner-db]
  "Return characters that are considered learnt (difficulty < 0.3)"
  (->> learner-db
       (filter (fn [[_char stat]] (learner/seems-learnt? stat)))
       (map first)))

(defn chars-for-continue-review [learner-db]
  "Return characters suitable for continue review - prioritize less mastered characters"
  (let [all-chars (->> learner-db
                       (map (fn [[char stat]] [char stat]))
                       (sort-by (fn [[_char stat]] (:difficulty stat)) >) ; Sort by difficulty (high to low)
                       vec) ; Convert to vector for easier manipulation
        ; Step 1: Take 10 hardest characters (difficulty > 0.5)
        hardest-chars (->> all-chars
                          (filter (fn [[_char stat]] (> (:difficulty stat) 0.5)))
                          (take 10))
        ; Step 2: Add 5 medium-difficulty characters (0.1 < difficulty < 0.5)
        medium-chars (->> all-chars
                         (filter (fn [[_char stat]] (and (> (:difficulty stat) 0.1)
                                                       (<= (:difficulty stat) 0.5))))
                         (take 5))
        ; Step 3: Fill up to 20 total with easier characters if needed
        easier-chars (->> all-chars
                         (filter (fn [[_char stat]] (<= (:difficulty stat) 0.1)))
                         (take (- 20 (+ (count hardest-chars) (count medium-chars)))))
        selected-chars (concat hardest-chars medium-chars easier-chars)]
    (map first selected-chars)))

(defn set-chars-due-now [learner-db char-list]
  "Set the due date of characters to now, making them available for review"
  (reduce (fn [db char]
            (if (contains? db char)
              (let [current-stat (get db char)
                    ; Set dbr to 0 so the character is immediately due for review
                    updated-stat (assoc current-stat :dlr (js/Date.now) :dbr 0)]
                (assoc db char updated-stat))
              db))
          learner-db char-list))

(defn prompt-learn-more? [learner-db add-count]
  (when (seq (new-chars-to-learn add-count learner-db))
    (let [num-learnt (->> learner-db
                          vals
                          (filter learner/seems-learnt?)
                          count)]
      (>= num-learnt (* (count learner-db) 0.8)))))
#_(can-learn-more? @*learner-db)


;;; Chinese character input Q&A

(defn next-question [model learner-db]
  ;; sort DB to show upcoming card, by how much need to review
  (if-let [review-items (seq (items-to-review learner-db))]
    (let [new-char (->> review-items (sort-by val learner/compare-stat)
                        first key)
          radicals (split-radicals new-char)
          hint-count (if (:practice-mode? model) (count radicals) 0)]
      (assoc model :question-char new-char :ans-parts [] :hint-count hint-count
             :parts-score (-> new-char split-radicals count (repeat 0) vec)
             :answered? false
             :question-start-time (js/Date.now)
             :viz-page-size viz-page-size-compact))
    (do (log "no item to review!")
        ; When no more items to review, start a new session for next round
        (start-new-session!)
        (assoc model :question-char nil :ans-parts [] :hint-count 0
               :parts-score nil :answered? false
               :viz-page-size viz-page-size-long :viz-page 1))))
#_(->> @*learner-db
       (sort-by val compare-stat)
       (filter (fn [[_char stat]] (need-review? stat))))

(defn new-model [learner-db]
  (-> {:viz-page-size (if (seq (items-to-review learner-db))
                        viz-page-size-compact
                        viz-page-size-long)
       :viz-page 1
       :show-stats? false
       :practice-mode? false} ; Add practice mode state
      (merge {:question-char nil
              :ans-parts []
              :hint-count 0
              :parts-score []
              :answered? false})
      (next-question learner-db)))

(defn score-part [parts-score part-index correct?]
  (let [score (nth parts-score part-index)]
    (assoc parts-score part-index
           (if (zero? score) ; part score default zero = scorable
             ; +1 for correct part answer, -1 if incorrect
             (if correct? (inc score) (dec score))
             ; don't update score if part has been answered
             score))))

(defn score-part-hint [parts-score part-index]
  (let [score (nth parts-score part-index)]
    (assoc parts-score part-index
           (if (zero? score) ; part score default zero = scorable
             -0.5 ; -0.5 for asking hint
             ; don't update score if part has been answered
             score))))

(defn has-next-page? [{:keys [viz-page-size viz-page]} learner-db]
  (< (* viz-page viz-page-size) (count learner-db)))