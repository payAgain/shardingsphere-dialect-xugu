# XuGu ExceptionMapper Error Code Map

> Module: `database-exception-xugu`  
> Mapper: `XuguSQLDialectExceptionMapper`  
> Catalog: `XuguVendorError`  
> Goal: G-004 P1-4

## Mapping policy

XuGu JDBC (`xugu-jdbc`) does **not** publish a stable public vendor-code catalog aligned with ShardingSphere dialect exceptions. This dialect follows the same pattern as ShardingSphere’s PostgreSQL mapper:

| Field | Value |
|---|---|
| SQLState | XOpen / SQL:2003 class via `XOpenSQLState` |
| Vendor code | always `0` (no invented XuGu server codes) |
| Reason | human-readable English message with `String.format` placeholders |

Driver-local client diagnostics (`E5xxxx` / `E51xxx` in `com.xugu.cloudjdbc.*`) are **not** remapped here; they already surface as JDBC `SQLException` from the driver.

## Mapped codes (`XuguVendorError`)

Count: **20** enum constants (19 dialect mappings + `GENERAL_ERROR` fallback reason template).

| # | Enum | SQLState | Vendor | Typical trigger (`SQLDialectException`) | Reason template |
|---|---|---|---|---|---|
| 1 | `UNKNOWN_DATABASE` | `3D000` | 0 | `UnknownDatabaseException` (named) | Unknown database '%s' |
| 2 | `NO_DATABASE_SELECTED` | `3D000` | 0 | `UnknownDatabaseException`(null) / `NoDatabaseSelectedException` | No database selected |
| 3 | `DATABASE_EXISTS` | `HY000` | 0 | `DatabaseCreateExistsException` | Can't create database '%s'; database exists |
| 4 | `DATABASE_DROP_NOT_EXISTS` | `HY000` | 0 | `DatabaseDropNotExistsException` | Can't drop database '%s'; database doesn't exist |
| 5 | `TABLE_EXISTS` | `42S01` | 0 | `TableExistsException` | Table '%s' already exists |
| 6 | `NO_SUCH_TABLE` | `42S02` | 0 | `NoSuchTableException` | Table '%s' doesn't exist |
| 7 | `COLUMN_NOT_FOUND` | `42S02` | 0 | `ColumnNotFoundException` | Unknown column '%s' in table '%s' |
| 8 | `PARSE_ERROR` | `42000` | 0 | `DialectSQLParsingException` | %s near '%s' at line %d |
| 9 | `INSERT_COLUMNS_VALUES_MISMATCH` | `21S01` | 0 | `InsertColumnsAndValuesMismatchedException` | Column count doesn't match value count at row %d |
| 10 | `INVALID_PARAMETER_VALUE` | `22023` | 0 | `InvalidParameterValueException` | Invalid value for parameter '%s': '%s' |
| 11 | `DUPLICATE_KEY` | `23000` | 0 | `DuplicateKeyException` (XuGu dialect) | Duplicate key value violates unique constraint '%s' |
| 12 | `NULL_NOT_ALLOWED` | `23000` | 0 | `NullNotAllowedException` (XuGu dialect) | NULL not allowed for column '%s' |
| 13 | `ACCESS_DENIED` | `28000` | 0 | `AccessDeniedException` | Access denied for user '%s'@'%s' (using password: %s) |
| 14 | `TOO_MANY_CONNECTIONS` | `08004` | 0 | `TooManyConnectionsException` | Too many connections |
| 15 | `CONNECTION_FAILURE` | `08000` | 0 | `ConnectionFailedException` (link=false) | Connection failure: %s |
| 16 | `COMMUNICATION_LINK_FAILURE` | `08S01` | 0 | `ConnectionFailedException` (link=true) | Communication link failure: %s |
| 17 | `QUERY_TIMEOUT` | `HY000` | 0 | `QueryTimeoutException` (XuGu dialect) | Query timed out after %d second(s) |
| 18 | `TRANSACTION_STATE_INVALID` | `25000` | 0 | `InTransactionException` | There is already a transaction in progress |
| 19 | `TABLE_MODIFY_IN_TRANSACTION` | `25000` | 0 | `TableModifyInTransactionException` | Table '%s' cannot be modified in the current transaction state |
| 20 | `GENERAL_ERROR` | `HY000` | 0 | (reason template / docs only) | %s |

Unmapped `SQLDialectException` subtypes fall through to `UnknownSQLException`.

## Common server / JDBC observation notes

Baseline IT (`tests-it`) asserts `SQLException` on duplicate PK inserts but does **not** pin XuGu server SQLState/vendor integers (they vary by message path). Recommended client handling:

| Scenario | Prefer SQLState class | Notes |
|---|---|---|
| Duplicate PK / unique | `23000` | Aligns with `DUPLICATE_KEY` |
| NOT NULL | `23000` | Aligns with `NULL_NOT_ALLOWED` |
| Syntax | `42000` | Aligns with `PARSE_ERROR` |
| Connection refused / closed | `08000` / `08S01` | Aligns with connection failure entries |
| Statement timeout | `HY000` (no dedicated XOpen timeout in SS `XOpenSQLState`) | Aligns with `QUERY_TIMEOUT` |
| Tx misuse | `25000` | Aligns with transaction entries |

## Driver client codes (reference only)

Frequent `E5xxxx`/`E51xxx` markers appear inside `xugu-jdbc` client classes (parameter range, type conversion, URL/IO). Examples observed in `xugu-jdbc-12.3.6`: `E50007` (parameter index), `E50020`/`E50026` (connection IO), `E50044` (type conversion). These are **not** part of `XuguVendorError` and are not remapped by `SQLDialectExceptionMapper`.

## Related

- [support-matrix.md](support-matrix.md) — SQLException mapping row  
- Upstream patterns: MySQL `MySQLVendorError` (numeric vendor codes) · PostgreSQL `PostgreSQLVendorError` (SQLState + vendor 0)
