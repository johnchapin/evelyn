(ns evelyn.models.tick
  (:refer-clojure :exclude [load])
  (:import [org.joda.time DateTime DateMidnight DateTimeZone])
  (:require [evelyn.entities :as entities]
            [evelyn.tick-processor.google]
            [clojure.java.io :as jio]
            [clojure.tools.logging :as log]
            [korma.core :refer :all]))

(defn- weekday? [datetime]
  (<= (.getDayOfWeek datetime) 5))

(defn- preceding-weekday [datetime]
  (let [preceding-day (.minusDays datetime 1)]
    (if (weekday? preceding-day)
      preceding-day
      (preceding-weekday preceding-day))))

(defn most-recent [symbol]
  (first
    (select entities/ticks
            (where {:symbol (name symbol)})
            (order :time :DESC)
            (order :id :DESC) (limit 1))))

(let [null-writer (proxy [java.io.Writer] []
                    (write
                      ([_ _ _] nil)
                      ([_] nil))
                    (flush [] nil)
                    (close [] nil))

      merge-stmt-prefix (str "MERGE INTO \"ticks\" (\"symbol\", \"time\", \"close\", \"high\", \"low\", \"open\", \"volume\")"
                             " KEY (\"symbol\", \"time\") VALUES ")

      merge-stmt-suffix "(?,?,?,?,?,?,?)"]

  (defn add [vals]
        (try
          (binding [*out* null-writer]
            (let [merge-stmt (apply str (concat [merge-stmt-prefix]
                                                (interpose "," (repeat (count vals) merge-stmt-suffix))))
                  merge-vals (apply concat
                                    (map #(identity
                                            [(name (:symbol %)) (.getMillis (:time %)) (:close %) (:high %) (:low %) (:open %) (:volume %)])
                                         vals))]

              (exec-raw [merge-stmt merge-vals] :result)))
          (catch java.sql.SQLException e
            (let [[short-message & _] (clojure.string/split (.getMessage e) #";")]
              (log/warn :add short-message))))))

;; Are these ticks representative of the same prices at
;; the same moment in time? The only allowable difference
;; is the volume.
(defn- valid? [ticks]
  (apply = (map #(dissoc % :volume) ticks)))

;; Highest volume wins
(defn- collapse-ticks [ticks]
  (when (valid? ticks)
    (apply merge (sort-by :volume ticks))))

(defn dedupe-ticks [ticks]
  (let [ticks-by-time (group-by :time ticks)]
    (sort-by :time (map #(collapse-ticks (val %)) ticks-by-time))))

(def processors [(evelyn.tick-processor.google/->GoogleTickProcessor)])

(defn process-file [remaining-processors file]
  (when-let [processor (first remaining-processors)]
    (log/info :process-file processor file)
    (if (.canProcess? processor file)
      (.process processor file)
      (recur (rest remaining-processors) file))))

(defn download [remaining-processors symbol]
  (when-let [processor (first remaining-processors)]
    (log/info :download processor symbol)
    (if (.canDownload? processor symbol)
      (.download processor symbol)
      (recur (rest remaining-processors) symbol))))

(defn load-files [symbol]
  (let [dirname (str "data/" (name symbol))
        dir (jio/file (jio/resource dirname))]
    (when (and dir (.exists dir))
      (let [files (remove #(.isDirectory %) (file-seq dir))]
        (log/info :load-files files)
        (when-not (empty? files)
          (->> files
               (mapcat (partial process-file processors))
               dedupe-ticks
               (map #(assoc % :symbol symbol))))))))

(let [eastern (DateTimeZone/forID "US/Eastern")]
  (defn update? [symbol]
    ;; Determine the most recent available tick in the database
    (let [last-weekday (preceding-weekday (DateMidnight. eastern))
          ;; Trading day (for NASDAQ) is 09:30 to 16:00 EST
          last-eot (.withZone (.withHourOfDay (DateTime. last-weekday) 16) DateTimeZone/UTC)
          last-tick (most-recent symbol)]
      (log/debug :last-eot last-eot :last-tick (:time last-tick))
      ;; If that tick is before the EOT (end of trading) of the
      ;; preceding weekday, or doesn't exist (empty DB), load
      ;; from files.
      (or (nil? (:time last-tick))
          (.isBefore (:time last-tick) last-eot)))))

(defn backload [symbol]
  ;; We want to load ticks up through the most recent
  ;;  preceding weekday. Try our local filesystem first,
  ;;  then if we still don't have recent enough data,
  ;;  try downloading. If that doesn't work either,
  ;;  give some warning but don't error out.

  (when (update? symbol)
    (let [ticks (load-files symbol)]
      (when-not (empty? ticks)
        (log/debug :count-ticks (count ticks))
        (log/debug :first-ticks (first ticks))
        (log/spy :debug (add ticks)))))

  (when (update? symbol)
    (let [ticks (try
                  (download processors symbol)
                  (catch Exception e
                    (log/warn e)
                    []))]
      (when-not (empty? ticks)
        (log/debug :count-ticks (count ticks))
        (log/debug :first-ticks (first ticks))
        (log/debug :last-ticks (last ticks))
        (log/spy :debug (add ticks)))))

  (when (update? symbol)
    (log/warn "Incomplete data for " symbol)))

(defn load
  ([symbol]
   (load symbol 0 (System/currentTimeMillis)))
  ([symbol datetime-from datetime-to]
   (backload symbol)
   (select entities/ticks
           (where {:symbol (name symbol)})
           (where (between :time [datetime-from datetime-to]))
           (order :time :ASC))))
