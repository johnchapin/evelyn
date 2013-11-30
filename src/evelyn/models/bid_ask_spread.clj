(ns evelyn.models.bid-ask-spread
  (:import [org.joda.time DateTime DateTimeZone])
  (:refer-clojure :exclude [get])
  (:require [evelyn.entities :as entities]
            [evelyn.spread-processor.yahoo :as yahoo]
            [clojure.tools.logging :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]))

(defn- get-db [symbol]
  (let [now (DateTime. DateTimeZone/UTC)
        hour-ago (.minusHours now 1)]
    (first
      (select entities/bid-ask-spreads
              (where {:symbol (name symbol)})
              (where (between :time [(.getMillis hour-ago) (.getMillis now)]))
              (order :time :ASC)
              (limit 1)))))

(defn- load-db [symbol]
  (when-let [[bid ask] (yahoo/fetch-bid-ask symbol)]
    (when (not-any? zero? [bid ask])
      (insert entities/bid-ask-spreads
              (values {:symbol symbol
                       :time (DateTime.)
                       :bid bid
                       :ask ask
                       :spread (- ask bid)})))))

(defn get [symbol]
  (if-let [spread (get-db symbol)]
    spread
    (do
      (load-db symbol)
      (if-let [spread (get-db symbol)]
        spread
        (log/spy :warn {:spread 0.0})))))
