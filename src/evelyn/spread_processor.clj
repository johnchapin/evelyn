(ns evelyn.spread-processor)

(defprotocol SpreadProcessor
  (bid-ask [this symbol]))
