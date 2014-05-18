(ns foosball.console)

(defmacro log [& args]
  (let [av (vec args)]
    `(when foosball.console/*enable-console*
       (.apply js/console.log js/console (stringify ~av)))))

(defmacro info [& args]
  (let [av (vec args)]
    `(when foosball.console/*enable-console*
      (.apply js/console.info js/console (stringify ~av)))))

(defmacro debug [& args]
  (let [av (vec args)]
    `(when foosball.console/*enable-console*
       (.apply js/console.debug js/console (stringify ~av)))))

(defmacro debug-js [& args]
  (let [av (vec args)]
    `(when foosball.console/*enable-console*
       (.apply js/console.debug js/console (into-array ~av)))))

(defmacro error [& args]
  (let [av (vec args)]
    `(.apply js/console.error js/console (stringify ~av))))

(defmacro trace []
  `(when foosball.console/*enable-console* (.trace js/console)))
