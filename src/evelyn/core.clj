(ns evelyn.core
  (:import [org.joda.time DateTime])
  (:require [evelyn.fitness.ts-buy-sell-stop :as trade-series]
            [evelyn.init]
            [evelyn.models.result :as result]
            [evelyn.models.bid-ask-spread :as bid-ask-spread]
            [evelyn.tick-processor.yahoo :as yahoo]
            [evelyn.tick-series :as tick-series]

            [pso.swarm]
            [pso.dimension]

            [clojure.tools.logging :as log]
            ))

(defn init! []
  @(delay (evelyn.init/init!)))

(defn optimize-trade-series [symbol & {:keys [starting-capital capital-pct
                                              transaction-fee swarm-size generations
                                              days]
                                       :or {starting-capital 5000.0
                                            capital-pct 1.0
                                            transaction-fee 0.0
                                            swarm-size 30
                                            generations 100
                                            days 30}}]

  (let [time-to (-> (DateTime.) .getMillis)
        time-from (-> (DateTime. time-to)
                      (.minusDays ,,, days)
                      .getMillis)]

    (log/info :time-from time-from :time-to time-to)

    (init!)

    (when-let [tick-series (tick-series/factory symbol time-from time-to)]

      (log/info (str tick-series))

      (let [open-high-spreads (sort
                                (map (fn [[k v]]
                                       (let [day-open (:open (first v))
                                             day-high (reduce max (map :high v))]
                                         (- day-high day-open)))
                                     (:ticks tick-series)))

            median-idx (int (/ (count open-high-spreads) 2))
            median-high-open-spread (get (vec open-high-spreads) median-idx)

            ba-spread (or (bid-ask-spread/get symbol) 0.0)

            fitness-fn (partial trade-series/fit starting-capital capital-pct transaction-fee ba-spread tick-series)

            dimensions [(pso.dimension/build :buy 0.0 median-high-open-spread)
                        (pso.dimension/build :sell (+ 0.01 median-high-open-spread) (* 2 median-high-open-spread))
                        (pso.dimension/build :stop (* -1 median-high-open-spread) median-high-open-spread)]

            swarm (atom (pso.swarm/build dimensions fitness-fn trade-series/compare-results swarm-size))]

        (vec (repeatedly generations #(swap! swarm pso.swarm/update)))
        (result/add (:value (:result (:best-particle @swarm))))))))

(defn go [& args]
  (let [symbol-data (slurp (clojure.java.io/resource "nasdaq.100.symbols"))
        symbols (map keyword (clojure.string/split symbol-data #" "))]
    (for [symbol symbols]
      (apply optimize-trade-series symbol args))))
