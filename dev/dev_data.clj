(ns dev-data)

(def example-matches
  [{:id 333, :matchdate #inst "2013-04-11",
    :team1 {:score 10, :player2 "Knud Erik", :player1 "Lisse", :id 5555},
    :team2 {:score 7, :player2 "Thomas", :player1 "Anders", :id 6666}}
   {:id 222, :matchdate #inst "2013-04-12",
    :team1 {:score 10, :player2 "Thomas", :player1 "Maria", :id 3333},
    :team2 {:score 2, :player2 "Lisse", :player1 "Anders", :id 4444}}
   {:id 111, :matchdate #inst "2013-04-13",
    :team1 {:score 10, :player2 "Lisse", :player1 "Thomas", :id 1111},
    :team2 {:score 8, :player2 "Maria", :player1 "Anders", :id 2222}}
   {:id 444, :matchdate #inst "2013-04-15",
    :team1 {:score 10, :player2 "Anders", :player1 "Knud Erik", :id 7777},
    :team2 {:score 6, :player2 "Lisse", :player1 "Maria", :id 8888}}])
