(ns foosball.spinners)

(defn spinner []
  [:div.spinner
   (map (fn [i] [:div.circle {:class (str "circle" (inc i))}]) (range 12))])

(defn loading []
  [:div
   (spinner)
   [:p.small.text-center "Loading Foosball"]])
