# Modules

## Layout (parent POM)

| Module | Role |
|--------|------|
| `connector-xugu` | `DatabaseType`, URL, metadata, ResultSet |
| `parser-sql-statement-xugu` | Statement types |
| `parser-sql-engine-xugu` | ANTLR + visitors + facade SPI |
| `infra-binder-xugu` | Binder dialect |
| `infra-route-xugu` | Route dialect |
| `infra-rewrite-xugu` | Rewrite dialect |
| `sharding-dialect-xugu` | Sharding feature dialect (pagination merge etc.) |
| `sql-federation-xugu` | Federation stubs |
| `transaction-xugu` | Savepoint / XA helpers |
| `database-exception-xugu` | SQLException mapper |
| `jdbc-dialect-xugu` | Aggregation artifact (user dependency) |
| `proxy-backend-xugu` | Proxy storage dialect (MySQL wire frontend + XuGu JDBC) |
| `proxy-dialect-xugu` | Proxy aggregation POM |
| `tests-it` | Integration tests |

## Rules

- User-facing JDBC dependency: `shardingsphere-jdbc-dialect-xugu`.
- Do not put business logic in aggregation POMs.
- New SPI capability → new/updated module + parent `<modules>` entry + aggregation runtime dep when needed.
