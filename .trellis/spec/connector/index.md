# Connector

## Package

`com.xugudb.shardingsphere.database.connector.xugu`

## Key class

`XuguDatabaseType` — prefixes `jdbc:xugu:`, type `"XuGu"`, **no trunk override**.

## Rules

- Default port / schema / system views must match XuGu driver behavior (`docs/` + lab).
- Metadata SQL uses XuGu catalogs (`ALL_*`), not MySQL `information_schema`.
- Evidence priority: JDBC driver > docs > historical harness notes.
