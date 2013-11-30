(ns evelyn.tick-processor.google
  (:refer-clojure :exclude [new])
  (:import [org.joda.time DateTime DateTimeZone])
  (:require [evelyn.tick-processor]
            [clojure.java.io :as jio]
            [clojure.string]
            [clojure.tools.logging :as log]))

(defn- parse-header-line-long [line]
  (Long/parseLong (clojure.string/replace line #".*=" "")))

(defn- process-raw [raw]
  (let [lines (clojure.string/split-lines raw)
        [[_ market-open-minute market-close-minute interval-seconds _ _ timezone-offset-minutes]
         data-lines] (split-with (partial re-matches #"^[A-Z].*") lines)
        market-open-minute* (parse-header-line-long market-open-minute)
        market-open-second (* 60 market-open-minute*)
        interval-seconds* (parse-header-line-long interval-seconds)
        interval-millis (* 1000 interval-seconds*)
        timezone-offset-minutes* (parse-header-line-long timezone-offset-minutes)
        timezone-offset-seconds (* 60 timezone-offset-minutes*)
        running-millis (atom 0)
        ]
    ;; DATE,CLOSE,HIGH,LOW,OPEN,VOLUME
    ;; 1363354200,437.99,438.32,437.8,437.93,2257435
    (for [data-line data-lines]
      (let [[tick-delta & nums] (clojure.string/split data-line #",")
            nums* (take 5 (concat nums (repeat 4 "0")))
            matcher (re-matcher #"^a([0-9]+)" tick-delta)
            tick-millis (if-not (empty? (re-find matcher))
                          (let [running-millis* (* (- (Long/parseLong (second (re-groups matcher)))
                                                     timezone-offset-seconds) ;; Correct to UTC
                                                   1000)]                     ;; and to millis
                            (reset! running-millis running-millis*))
                          (+ @running-millis (* interval-millis (Long/parseLong tick-delta))))
            tick-time (DateTime. tick-millis DateTimeZone/UTC)]
        (apply evelyn.tick-processor/->Tick tick-time (map #(Double/parseDouble %) nums*))))))

(let [url-fmt (str "https://www.google.com/finance/"
                   "getprices?i=60&p=365d&f=d,o,h,l,c,v&df=cpct&q=%s")]
  (defn- fetch-raw [symbol]
    (let [url (format url-fmt (name symbol))
          raw (slurp url)]
      (map #(assoc % :symbol symbol)
           (process-raw raw)))))

(defn- fetch-and-process [symbol]
  (map #(assoc % :symbol symbol)
       (-> symbol
           fetch-raw
           process-raw)))

(deftype GoogleTickProcessor []
  evelyn.tick-processor/TickProcessor
  (canDownload? [_ symbol]
    true)
  (canProcess? [_ file]
    true)
  (download [_ symbol]
    (fetch-and-process symbol))
  (process [_ file]
    (when-let [raw (slurp file)]
      (process-raw raw))))

(defn new []
  (GoogleTickProcessor.))

