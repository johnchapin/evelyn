(ns evelyn.state)

(defmacro transition [fn-name state-atom begin-state end-state & forms]
  `(defn ~fn-name []
     (let [transition-state# [~begin-state ~end-state]]
       (when (compare-and-set! ~state-atom ~begin-state transition-state#)
         (if (do ~@forms)
           (compare-and-set! ~state-atom transition-state# ~end-state)
           (compare-and-set! ~state-atom transition-state# ~begin-state)))
       (deref ~state-atom))))
