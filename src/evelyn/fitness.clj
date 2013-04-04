(ns evelyn.fitness)

(defn three [& args]
  (- (Math/abs 3) (Math/abs (apply * args))))
