(ns hub.db
  "SQLite access via next.jdbc, with HugSQL for queries and Migratus for schema
   migrations. SQLite keeps the appliance to an app plus a file (see README)."
  (:require [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]
            [migratus.core :as migratus]
            [next.jdbc :as jdbc]))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

;; Defines `upsert-inverter-agg15!` from the SQL on the classpath.
(declare upsert-inverter-agg15!)
(hugsql/def-db-fns "sql/inverter.sql")

(defn datasource
  "A SQLite datasource with WAL journaling and foreign keys enabled. WAL is a
   persistent property of the database file; foreign-key enforcement is applied
   per connection via the JDBC URL pragmas understood by sqlite-jdbc."
  [{:keys [path]}]
  (jdbc/get-datasource
   (str "jdbc:sqlite:" path "?journal_mode=WAL&foreign_keys=true")))

(defn migrate!
  "Runs pending migrations from resources/migrations."
  [datasource]
  (migratus/migrate {:store :database
                     :migration-dir "migrations"
                     :db {:datasource datasource}}))

(defn store-agg15!
  "Persists one 15-minute inverter aggregate. SQLite stores the boolean
   `complete` flag as an integer 0/1."
  [datasource agg]
  (upsert-inverter-agg15! datasource (update agg :complete #(if % 1 0))))
