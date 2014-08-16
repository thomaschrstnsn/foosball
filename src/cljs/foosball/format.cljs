(ns foosball.format
  (:require [goog.string :as gstring]
            [goog.string.format]
            [foosball.routes :as routes]))

(defn format
  "Formats a string using goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))

(defn format-percentage
  ([p] (format-percentage 1 p))
  ([digits p] (format (str "%." digits "f%%") (double p))))

(defn format-rating [r]
  (format "%.1f" (double r)))

(defn format-player-link [players playername]
  (let [player (->> players
                   (filter (fn [{:keys [name]}] (= name playername)))
                   first)]
    [:a {:href (routes/player-log-path {:player-id (:id player)})} (:name player)]))

(defn format-team-links [players team]
  [:span (->> (map (partial format-player-link players) team)
              (interpose ", "))])

(defn style-value
  "Formats a value with text-success and text-error classes, based on optional checker.
   Defaults to pos? such that positive numbers are text-success and negative are text-error.
   Values failing the optional class? predicate are not given a class.
   Default 0 is not given a class. With class? nil everything is given a class
   Optional argument printer is used to format value to string, defaults to str"
  [d & {:keys [checker class? printer container-tag] :or {checker       pos?
                                                          class?        (partial not= 0)
                                                          printer       str
                                                          container-tag :div}}]
  [container-tag
   (when (or (nil? class?) (class? d))
     {:class (if (checker d) "text-success" "text-danger")})
   (printer d)])

(defn style-score [s]
  (style-value s :checker (partial < 9) :class? nil))

(defn style-match-percentage [wins? p]
  (style-value p
                :printer format-percentage
                :class?  #(not= (double 50) (double p))
                :checker (if wins?
                           (partial < 50)
                           (partial > 50))))

(defn style-rating [r]
  (style-value r :printer format-rating :class? nil :checker (partial < 1500)))

(defn style-form [won lost form]
  (map #(style-value % :printer {won "W" lost "L"} :class? nil :checker (partial = won) :container-tag :span) form))
