(ns evelyn.entities
  (:import [org.joda.time DateTime DateTimeZone Duration])
  (:require [evelyn.util :as util]
            [clojure.tools.logging :as log]
            [korma.core :refer :all]))

(defn- mutate [entity k f]
  (merge entity
         (when-let [v (get entity k)]
           {k (f v)})))

(defn- incoming->millis [incoming k]
  (mutate incoming k (fn [t] (.getMillis t))))

(defn- outgoing-datetime [outgoing k]
  (mutate outgoing k (fn [t] (DateTime. t DateTimeZone/UTC))))

(declare ticks open-ticks close-ticks results trades)

(defentity ticks
  (prepare
    (fn [incoming]
      (-> incoming
          (mutate ,,, :symbol name)
          (mutate ,,, :time (fn [t] (.getMillis t)))
          (select-keys ,,, [:symbol :time :close :high :low :open :volume]))))

  (transform
    (fn [outgoing]
      (-> outgoing
          (mutate ,,, :symbol keyword)
          (mutate ,,, :time (fn [t] (DateTime. t DateTimeZone/UTC)))))))

(defentity results
  (has-many trades {:fk :result-id})
  (prepare
    (fn [incoming]
      (-> incoming
          (mutate ,,, :symbol name)
          (mutate ,,, :start-time (fn [t] (.getMillis t)))
          (mutate ,,, :duration (fn [d] (.getMillis d)))
          (mutate ,,, :trades (fn [ts] (count ts)))
          (mutate ,,, :bid-ask-spread :id)
          (select-keys ,,, [:symbol :start-time :duration :capital :capital-pct :transaction-fee :bid-ask-spread :profit :sharpe :buy :sell :stop :trades]))))

  (transform
    (fn [outgoing]
      (-> outgoing
          (mutate ,,, :symbol keyword)
          (mutate ,,, :start-time (fn [t] (DateTime. t DateTimeZone/UTC)))
          (mutate ,,, :duration (fn [d] (Duration. d)))))))

(defentity open-ticks
  (table :ticks :open-ticks))

(defentity close-ticks
  (table :ticks :close-ticks))

(defentity trades
  (belongs-to results {:fk :result-id})
  (belongs-to open-ticks {:fk :open-tick})
  (belongs-to close-ticks {:fk :close-tick})
  (prepare
    (fn [incoming]
      (-> incoming
          (select-keys ,,, [:result-id :open-tick :close-tick :shares])))))

(defentity bid-ask-spreads
  (prepare
    (fn [incoming]
      (-> incoming
          (mutate ,,, :symbol name)
          (mutate ,,, :time (fn [t] (.getMillis t)))
          (select-keys ,,, [:symbol :time :bid :ask :spread]))))
  (transform
    (fn [outgoing]
      (-> outgoing
          (mutate ,,, :symbol keyword)
          (mutate ,,, :time (fn [t] (DateTime. t DateTimeZone/UTC)))))))
