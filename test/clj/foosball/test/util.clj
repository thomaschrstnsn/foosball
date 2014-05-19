(ns foosball.test.util
  (:use midje.sweet foosball.util))

(facts "about symbols-as-map"
       (let [a "Abc"
             b "Bcd"
             c "Cde"]
         (symbols-as-map a b c) => {:a a :b b :c c}
         (symbols-as-map) => {}))
