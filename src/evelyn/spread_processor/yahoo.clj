(ns evelyn.spread-processor.yahoo
  (:require [evelyn.spread-processor]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(let [url-fmt "http://download.finance.yahoo.com/d/quotes.csv?s=%s&f=b2b3"]
  (defn fetch-bid-ask [symbol]
    (when-let [raw (try
                     (-> url-fmt
                         (format ,,, (name symbol))
                         slurp
                         string/trim)
                     (catch Exception e
                       (log/warn :fetch-bid-ask e)))]
      (log/info :fetch-bid-ask raw)
      (when (re-matches #"[\d\.]+,[\d\.]+.*" raw)
        (map #(Double/parseDouble %)
             (string/split raw #","))))))

(deftype YahooSpreadProcessor []
  evelyn.spread-processor/SpreadProcessor
  (bid-ask [_ symbol]
    (fetch-bid-ask symbol)))
