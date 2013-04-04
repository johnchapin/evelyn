(ns evelyn.fitness.ts-buy-sell-stop
  (:import [org.joda.time DateTime DateTimeZone Duration])
  (:require [evelyn.util :as util]
            [clojure.tools.logging :as log]))

;; Split the ticks into two lists:
;;
;; List A) ticks from the beginning of the series that
;;         have open prices less than the buy signal.
;;         This list is ignored.
;;
;; List B) ticks, starting with the first of the series
;;         to have an open price greater than or equal
;;         to the buy signal.
;;
;; The first item in List B is the 'buy tick'.
;;
;; Now, split the remaining items in List B into two
;; more lists:
;;
;; List C) ticks, starting with the tick after the
;;         'buy tick', that have opening prices less
;;         than the sell signal and greater than
;;         the stop signal. This list is also ignored.
;;
;; List D) ticks, starting with the first of List B to
;;         have an opening price greater than the
;;         sell signal.
;;
;; The first item in List D is the 'sell tick'.
;;
;; If no 'sell tick' is found, the last tick in the
;; series is used instead.

(defn- build-trade [starting-capital capital-pct transaction-fee bid-ask-spread buy-tick sell-tick]
  (let [transaction-fee-total (* transaction-fee 2)
        half-bid-ask (/ bid-ask-spread 2)
        buy-price (+ (:open buy-tick) half-bid-ask)
        sell-price (- (:open sell-tick) half-bid-ask)
        available-capital (- (* capital-pct starting-capital) transaction-fee-total)
        shares (Math/floor (/ available-capital buy-price))]
    (when (pos? shares)
      {:opened (:time buy-tick)
       :open-tick (:id buy-tick)
       :buy buy-price
       :closed (:time sell-tick)
       :close-tick (:id sell-tick)
       :sell sell-price
       :shares shares
       :profit (- (* (- sell-price buy-price)
                     shares)
                  transaction-fee-total)
       })))

(defn- process-day [capital capital-pct transaction-fee bid-ask-spread ticks buy sell stop]
  (let [day-open (:open (first ticks))
        buy-signal (+ day-open buy)
        sell-signal (+ day-open sell)
        stop-signal (+ day-open stop)

        [_ [buy-tick & sell-candidates]] (split-with #(< (:open %) buy-signal) ticks)
        [_ [sell-tick & _]]              (split-with #(> sell-signal (:open %) stop-signal)
                                                     sell-candidates)]

    (when (and buy-tick (not (util/last? ticks buy-tick)))
      (build-trade capital capital-pct transaction-fee bid-ask-spread buy-tick (or sell-tick (last sell-candidates))))))

(defn- process-series [starting-capital capital-pct transaction-fee bid-ask-spread tick-series buy sell stop & _]
  (reduce (fn [[trades capital] ticks]
            (if-let [trade (process-day capital capital-pct transaction-fee bid-ask-spread ticks buy sell stop)]
              [(conj trades trade) (+ capital (:profit trade))]
              [trades capital]))
          [[] starting-capital]
          (vals (:ticks tick-series))))

(defn fit [starting-capital capital-pct transaction-fee bid-ask-spread tick-series buy sell stop]
  (let [buy* (util/scrub buy)
        sell* (util/scrub sell)
        stop* (util/scrub stop)
        start-time (DateTime. DateTimeZone/UTC)
        [trades ending-capital] (process-series starting-capital capital-pct transaction-fee (:spread bid-ask-spread) tick-series buy* sell* stop*)
        end-time (DateTime. DateTimeZone/UTC)]
    {:symbol (:symbol tick-series)
     :start-time start-time
     :duration (Duration. start-time end-time)
     :capital starting-capital
     :capital-pct capital-pct
     :transaction-fee transaction-fee
     :bid-ask-spread bid-ask-spread
     :profit (util/scrub (- ending-capital starting-capital))
     :sharpe (util/sharpe-ratio (map :profit trades))
     :buy buy* :sell sell* :stop stop*
     :trades trades ;; (count trades)
     }))

(defn compare-results [{trades-a :trades sharpe-a :sharpe}
                       {trades-b :trades sharpe-b :sharpe}]
  (cond
    (or (every? empty? [trades-a trades-b])
        (every? nil? [sharpe-a sharpe-b])
        (= sharpe-a sharpe-b))
    0

    (or (empty? trades-b)
        (> sharpe-a sharpe-b))
    -1

    (or (empty? trades-a)
        (< sharpe-a sharpe-b))
    1))
