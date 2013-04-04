(ns evelyn.tick-series
  (:import [org.joda.time DateMidnight DateTimeZone])
  (:require [evelyn.models.tick :as tick]
            [clojure.tools.logging :as log]))

(defrecord TickSeries
  [symbol ticks begin end open high close]
  Object
  (toString [_]
    (format "%s [%s -> %s] [%d days, %d ticks] [%f %f %f]"
            symbol begin end (count ticks) (reduce + (map count (vals ticks))) open high close)))

(defn factory [symbol datetime-from datetime-to]
  (let [distinct-ticks (tick/load symbol datetime-from datetime-to)
        ticks (group-by #(DateMidnight. (:time %) DateTimeZone/UTC) distinct-ticks)
        ;; Add {:last ...} metadata to each tick vector, to ease 'last?' processing later
        ticks* (into (sorted-map)
                     (reduce-kv (fn [m k v] (assoc m k (with-meta v {:last (last v)}))) {} ticks))
        begin (:time (first distinct-ticks))
        end (:time (last distinct-ticks))
        open (:open (first distinct-ticks))
        close (:close (last distinct-ticks))
        high (when-not (empty? distinct-ticks)
               (apply max (map :high distinct-ticks)))]
    (->TickSeries symbol ticks* begin end open high close)))
