(ns evelyn.init
  (:require [lobos.core]
            [lobos.migrations]))

(defn init! []
  (lobos.migrations/init!)
  (lobos.core/migrate))
