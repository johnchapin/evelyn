(ns evelyn.store
  (:require [evelyn.state :refer [transition]]
            [lobos.core]
            [lobos.migrations]))

;; Using a pseudo-DFA instead of a promise or delay,
;;  because I anticipate more complicated init/shutdown
;;  procedures and want to be able to cycle the data
;;  store while the app is running.

(let [state (atom :SHUTDOWN)]

  (transition init!
    state :SHUTDOWN :INIT
    (lobos.migrations/init!)
    (lobos.core/migrate)
    true)

  (transition shutdown!
    state :INIT :SHUTDOWN
    true))
