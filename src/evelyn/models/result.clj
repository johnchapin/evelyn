(ns evelyn.models.result
  (:require [evelyn.entities :as entities]
            [clojure.tools.logging :as log]
            [korma.core :refer :all]
            [korma.db :refer :all]))

(defn add [{:keys [trades] :as result}]
  (when-not (empty? trades)
    (transaction
      (try
        (when-let [result-id (val (first (insert entities/results (values result))))]
          (let [trades* (map #(assoc % :result-id result-id) trades)]
            (insert entities/trades (values trades*))))
        (catch java.sql.SQLException e
          (rollback)
          (let [[short-message & _] (clojure.string/split (.getMessage e) #";")]
            (log/warn :add short-message)))))))

;(pp/pprint
  ;(korma.core/select
    ;evelyn.entities/trades
    ;(korma.core/with evelyn.entities/open-ticks
      ;(korma.core/fields [:id :open-tick-id
                          ;:open :open-open
                          ;:close :open-close]))
    ;(korma.core/with evelyn.entities/close-ticks
      ;(korma.core/fields [:id :close-tick-id
                          ;:open :close-open
                          ;:close :close:close]))))
