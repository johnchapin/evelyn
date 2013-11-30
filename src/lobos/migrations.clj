(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:require [evelyn.config]
            [korma.db]
            [lobos.connectivity]
            [lobos.core :refer :all]
            [lobos.migration :refer [defmigration]]
            [lobos.schema :refer :all]))

(defn init! []
  ; Bit of a hack to re-use a connection from Korma for Lobos migrations...
  (korma.db/defdb db evelyn.config/db)
  (swap! lobos.connectivity/global-connections assoc
         :default-connection {:connection (korma.db/get-connection @korma.db/_default)
                              :db-spec evelyn.config/db}))

(defmigration add-ticks-table
  (up []
      (create
        (table :ticks
               (bigint :id :auto-inc :primary-key)
               (varchar :symbol 32)
               (bigint :time :not-null)
               (double :close)
               (double :high)
               (double :low)
               (double :open)
               (bigint :volume)))
      (create
        (index :ticks :ticks_symbol_time [:symbol :time] :unique)))

  (down []
        (drop (index :ticks_symbol_time))
        (drop (table :ticks))))

(defmigration add-results-table
  (up []
      (create
        (table :results
               (bigint :id :auto-inc :primary-key)
               (varchar :symbol 32)
               (bigint :start-time :not-null)
               (bigint :duration :not-null)
               (double :capital)
               (double :capital-pct)
               (double :transaction-fee)
               (bigint :bid-ask-spread)
               (double :profit)
               (double :sharpe)
               (double :buy)
               (double :sell)
               (double :stop)
               (integer :trades))))
  (down []
        (drop (table :results))))

(defmigration add-trades-table
  (up []
      (create
        (table :trades
               (bigint :id :auto-inc :primary-key)
               (bigint :result-id)
               (bigint :open-tick)
               (bigint :close-tick)
               (integer :shares :not-null))))
  (down []
      (drop (table :trades))))

(defmigration add-bid-ask-spreads-table
  (up []
      (create
        (table :bid-ask-spreads
               (bigint :id :auto-inc :primary-key)
               (varchar :symbol 32)
               (bigint :time :not-null)
               (double :bid)
               (double :ask)
               (bigint :spread))))
  (down []
      (drop (table :bid-ask-spreads))))
