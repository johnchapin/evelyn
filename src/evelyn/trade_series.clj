(ns evelyn.trade-series
  (:require [clojure.tools.logging :as log]
            [evelyn.fitness.ts-buy-sell-stop :as fitness]
            [evelyn.models.bid-ask-spread :as models.spread]
            [evelyn.models.result :as models.result]
            [evelyn.store :as store]
            [evelyn.tick-series :as tick-series]
            [evelyn.util :as util]
            [pso.dimension]
            [pso.swarm])
  (:import [org.joda.time DateTime]))

(defn- build-to-from
  "Given a positive number of days, returns a tuple of millisecond
  values representing [now, now - days]."
  [days]
  ^{:pre [pos? days]}
  (let [now (DateTime.)]
    [(.getMillis now)
     (.getMillis (.minusDays now days))]))

(defn- build-spreads
  "Given a tick series, returns a list of the daily open/high spreads."
  [{ticks :ticks}]
  (sort (map (fn [[_ day-ticks]]
               (let [day-open (:open (first day-ticks))
                     day-high (reduce max (map :high day-ticks))]
                 (- day-high day-open)))
             ticks)))

(defn- build-dimensions
  "Given pairs of min/max values, returns a list of pso.dimensions
  representing buy, stop, and sell."
  [buy-min buy-max sell-min sell-max stop-min stop-max]
  [(pso.dimension/build :buy buy-min buy-max)
   (pso.dimension/build :sell sell-min sell-max)
   (pso.dimension/build :stop stop-min stop-max)])

(defn optimize
  "Given a symbol, optimizes buy/sell/stop signals over the given
  (or default) time period."
  [symbol & {:keys [starting-capital capital-pct
                    transaction-fee swarm-size generations
                    days]
             :or {starting-capital 5000.0 capital-pct 1.0
                  transaction-fee 0.0 swarm-size 30
                  generations 100 days 30}}]

  (evelyn.store/init!)

  (let [[time-to time-from] (build-to-from days)]

    (when-let [tick-series (tick-series/factory symbol time-from time-to)]
      (log/info :tick-series (str tick-series))
      (when (pos? (count (:ticks tick-series)))
        (let [median-high-open-spread (-> tick-series build-spreads util/median)
              ba-spread (or (models.spread/get symbol) 0.0)
              fitness-fn (partial fitness/fit starting-capital capital-pct transaction-fee ba-spread tick-series)
              dimensions (build-dimensions 0.0 median-high-open-spread
                                           (+ 0.01 median-high-open-spread) (* 2 median-high-open-spread)
                                           (* -1 median-high-open-spread) median-high-open-spread)
              swarm (atom (pso.swarm/build dimensions fitness-fn fitness/compare-results swarm-size))]

          ;; Run and realize the PSO.
          (vec (repeatedly generations #(swap! swarm pso.swarm/update)))

          ;; Add results to the database
          (models.result/add (get-in @swarm [:best-particle :result :value])))))))
