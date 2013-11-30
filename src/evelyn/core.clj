(ns evelyn.core
  (:gen-class)
  (:require [evelyn.trade-series :as trade-series]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn -main [& args]
  (let [symbol-data (slurp (clojure.java.io/resource "nasdaq.100.symbols"))
        symbols (map (comp keyword string/trim) (clojure.string/split symbol-data #" "))]
    (for [symbol symbols]
      (apply trade-series/optimize symbol args))))
