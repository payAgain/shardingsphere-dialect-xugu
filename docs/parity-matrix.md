# XuGu Dialect SPI Parity Matrix (design §3.2 + G-003)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Design:** `sharding/docs/superpowers/specs/2026-07-20-xugu-native-jdbc-dialect-design.md` §3.2  
**G-003:** land XuGu-native-supported DEFERs; keep unsupported DEFER with reasons

Status: **PASS** · **DEFER** (XuGu-native unsupported / unmapped)

---

## B. Connector

| SPI | Pri | Status | Evidence |
|---|---|---|---|
| `DatabaseType` | P0 | PASS | `XuguDatabaseType` · SHA `65fc259` |
| `ConnectionPropertiesParser` | P0 | PASS | `XuguConnectionPropertiesParser` · SHA `65fc259` |
| `DialectDatabaseMetaData` | P0 | PASS | `XuguDatabaseMetaData` · XA class `com.xugu.xa.XADatasourceImp` |
| `DialectMetaDataLoader` | P0 | PASS | `XuguMetaDataLoader` · SHA `494ad80` |
| `DialectResultSetMapper` | P0 | PASS | `XuguResultSetMapper` · SHA `c386ba6` |
| `DialectSystemDatabase` | P1 | PASS | `XuguSystemDatabase` · SHA `eff8f22` |
| `DialectKernelSupportedSystemTable` | P1 | PASS | `XuguKernelSupportedSystemTable` · SHA `eff8f22` |
| `DialectDefaultQueryPropertiesProvider` | P1 | PASS | forces `compatiblemode=NONE` · SHA `cb7cabd` |
| `DialectDatabasePrivilegeChecker` | P1 | DEFER | XuGu privilege model not mapped to SS checker API; Oracle dialect also omits this SPI; inventing a no-op checker is forbidden |

---

## C–F. SQL stack

| Capability | Module | Pri | Status | Evidence |
|---|---|---|---|---|
| Parser Facade + Visitors | `parser-sql-engine-xugu` | P0 | PASS | SHA `4ab6ca0` |
| Statement types | `parser-sql-statement-xugu` | P0 | PASS | SHA `4ab6ca0` |
| Parser extensions (PL/SQL / cold DDL) | `parser-sql-engine-xugu` | P2 | DEFER | expand only as baseline SQL requires; full PL/SQL not XuGu-SS product scope yet |
| Binder projection | `infra-binder-xugu` | P0 | PASS | SHA `353ad82` |
| BindEngine | `infra-binder-xugu` | P1 | PASS | SHA `43887fe` |
| Route DAL | `infra-route-xugu` | P1 | PASS | SHA `9b3b2bd` |
| Rewrite | `infra-rewrite-xugu` | P1 | PASS | SHA `04c8651` |
| Federation connection config | `sql-federation-xugu` | P1 | PASS | SHA `c78651a` |
| Federation FunctionRegister | `sql-federation-xugu` | P2 | PASS | `XuguSQLFederationFunctionRegister` (safe empty; Oracle FUN via config) |
| Federation ColumnTypeConverter | `sql-federation-xugu` | P2 | PASS | `XuguSQLFederationColumnTypeConverter` |

---

## G. Feature

| Capability | Pri | Status | Evidence |
|---|---|---|---|
| `PaginationDecoratorMergedResultBuilder` | P0 | PASS | LIMIT · SHA `034e367` |
| `DialectShardingDALResultMerger` | P2 | DEFER | NONE mode has no MySQL-style SHOW DAL product need; no merger without SHOW surface |
| Encrypt / Readwrite dialect SPI | — | PASS | N/A — no independent dialect SPI |

---

## H. JDBC periphery

| Capability | Pri | Status | Evidence |
|---|---|---|---|
| `SQLDialectExceptionMapper` | P2 | PASS | `database-exception-xugu` · `XuguSQLDialectExceptionMapper` |
| XA `XAConnectionWrapper` | P2 | PASS | `transaction-xugu` · `XuguXAConnectionWrapper` → `com.xugu.xa.XAConnectionImp` |
| Savepoint SQL provider | P2 | PASS | `transaction-xugu` · `XuguSavepointReleaseSQLProvider` → `RELEASE SAVEPOINT %s` |

---

## Summary (G-003 Track1)

| Priority | PASS | DEFER |
|---|---|---|
| P0 | all | — |
| P1 | all except PrivilegeChecker | PrivilegeChecker |
| P2 | ExceptionMapper, XA, Savepoint, Federation Function/Converter | DAL ResultMerger, full parser PL/SQL |

**Track1 exit:** XuGu-native-supported periphery SPIs landed; remaining DEFERs have explicit XuGu-unsupported / out-of-scope reasons.
