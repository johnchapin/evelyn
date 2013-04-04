(ns evelyn.util
  (:require [evelyn.config]))

(defn symbols []
  (let [symbol-data (slurp (clojure.java.io/resource evelyn.config/symbol-filename))]
    (map keyword (clojure.string/split symbol-data #" "))))

(defn am-mean [samples]
  (/ (reduce + samples) (count samples)))

(defn std-dev [samples & {:keys [mean]}]
  (let [mean (or mean (am-mean samples))
        intermediate (map #(Math/pow (- %1 mean) 2) samples)]
    (Math/sqrt
      (/ (reduce + intermediate) (count samples)))))

(defn sharpe-ratio [samples]
  (if (empty? samples)
    0.00001
    (let [mean (am-mean samples)
          std-dev (std-dev samples :mean mean)
          std-dev* (if (zero? std-dev)
                     100.0
                     std-dev)]
      (/ mean std-dev*))))

(defn scrub [value]
  (-> value
      bigdec
      (.setScale ,,, 2 BigDecimal/ROUND_HALF_UP)))

(defn clob-to-string [clob]
  "Turn a JDBC Clob into a String"
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(defn last? [s i]
  (let [l (get (meta s) :last (last s))]
    (= i l)))
