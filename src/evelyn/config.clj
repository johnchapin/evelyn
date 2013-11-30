(ns evelyn.config)

(def symbol-filename "nasdaq.100.symbols")

(def db
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname "resources/data"})
