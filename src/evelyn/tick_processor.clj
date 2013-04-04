(ns evelyn.tick-processor)

(defrecord Tick
  [time close high low open volume])

(defprotocol TickProcessor
  (canProcess? [this files])
  (canDownload? [this symbol])
  (process [this files])
  (download [this symbol]))
