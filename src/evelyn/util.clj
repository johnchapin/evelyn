(ns evelyn.util
  (:require [evelyn.config]))

(defn symbols []
  (let [symbol-data (slurp (clojure.java.io/resource evelyn.config/symbol-filename))]
    (map keyword (clojure.string/split symbol-data #" "))))

(defn mean [nums]
  {:pre [every? number? nums]}
  (/ (apply + nums) (count nums)))

(defn median [samples]
  {:pre [every? number? samples]}
  (let [c (count samples)
        sorted (vec (sort samples))]
    (if (odd? c)
      (get sorted (/ (dec c) 2))
      (mean (let [mid (/ c 2)]
              (subvec sorted (dec mid) (inc mid)))))))

(defn std-dev [samples & {:keys [mean]}]
  (let [mean (or mean (mean samples))
        intermediate (map #(Math/pow (- %1 mean) 2) samples)]
    (Math/sqrt
      (/ (reduce + intermediate) (count samples)))))

(defn sharpe-ratio [samples]
  (if (empty? samples)
    0.00001
    (let [mean (mean samples)
          std-dev (std-dev samples :mean mean)
          std-dev* (if (zero? std-dev)
                     100.0
                     std-dev)]
      (/ mean std-dev*))))

(defn scrub [value]
  (-> value
      bigdec
      (.setScale ,,, 2 BigDecimal/ROUND_HALF_UP)))

(defn clob-to-string
  "Turn a JDBC Clob into a String"
  [clob]
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(defn last? [s i]
  (let [l (get (meta s) :last (last s))]
    (= i l)))

(defn weekday? [datetime]
  (<= (.getDayOfWeek datetime) 5))

(defn preceding-weekday [datetime]
  (let [preceding-day (.minusDays datetime 1)]
    (if (weekday? preceding-day)
      preceding-day
      (preceding-weekday preceding-day))))
