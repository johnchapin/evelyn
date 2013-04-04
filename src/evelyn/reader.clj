(ns evelyn.reader
  (:import [org.joda.time.format DateTimeFormat])
  (:require [clojure.string]))

(defrecord Tick [symbol time ask bid ask-volumne bid-volume])

(let [fmtr (DateTimeFormat/forPattern "dd.MM.YYYY HH:mm:ss.SSS")]
  (defn file->ticks [symbol filename]
    (when-let [raw (slurp filename)]
      (for [tl (rest (clojure.string/split-lines raw))]
        (let [[time & nums] (clojure.string/split tl #",")]
          (apply ->Tick symbol (.parseDateTime fmtr time) (map #(Double/parseDouble %) nums)))))))
