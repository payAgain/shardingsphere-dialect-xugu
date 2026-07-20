# XuGu Dialect SPI Parity Matrix (design §3.2)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Survey base:** `META-INF/services` under all modules + class presence (HEAD at M3-5 docs commit)  
**Design:** `sharding/docs/superpowers/specs/2026-07-20-xugu-native-jdbc-dialect-design.md` §3.2

Status values: **PASS** (SPI registered + unit coverage) · **DEFER** (documented reason; not blocking JDBC sharding path).

---

## B. Connector

| SPI | Pri | Status | Evidence |
|---|---|---|---|
| `DatabaseType` | P0 | PASS | `XuguDatabaseType` · `connector-xugu/.../DatabaseType` · SHA `65fc259` |
| `ConnectionPropertiesParser` | P0 | PASS | `XuguConnectionPropertiesParser` · SHA `65fc259` |
| `DialectDatabaseMetaData` | P0 | PASS | `XuguDatabaseMetaData` · SHA `c386ba6` |
| `DialectMetaDataLoader` | P0 | PASS | `XuguMetaDataLoader` · SHA `494ad80` |
| `DialectResultSetMapper` | P0 | PASS | `XuguResultSetMapper` · SHA `c386ba6` |
| `DialectSystemDatabase` | P1 | PASS | `XuguSystemDatabase` · SHA `eff8f22` |
| `DialectKernelSupportedSystemTable` | P1 | PASS | `XuguKernelSupportedSystemTable` · SHA `eff8f22` |
| `DialectDefaultQueryPropertiesProvider` | P1 | PASS | `XuguDefaultQueryPropertiesProvider` (`compatiblemode=NONE`) · SHA `cb7cabd` |
| `DialectDatabasePrivilegeChecker` | P1 | DEFER | deferred: XuGu privilege model not mapped; JDBC sharding path does not require this SPI (Oracle dialect also lacks `DialectDatabasePrivilegeChecker`); no fake checker invented |

---

## C–F. SQL stack

| Capability | Module | Pri | Status | Evidence |
|---|---|---|---|---|
| Parser Facade + Visitors (DML/DDL main path) | `parser-sql-engine-xugu` | P0 | PASS | `XuguParserFacade` + `XuguStatementVisitorFacade` · SPI `DialectSQLParserFacade` / `SQLStatementVisitorFacade` · SHA `4ab6ca0` |
| Statement types | `parser-sql-statement-xugu` | P0 | PASS | statement classes packaged with parser · SHA `4ab6ca0` |
| Parser extensions (complex PL/SQL, cold DDL) | `parser-sql-engine-xugu` | P2 | DEFER | deferred: not required for sharding JDBC main path; see `docs/syntax-whitelist-m1.md` deferred/unsupported columns |
| Binder projection extractor | `infra-binder-xugu` | P0 | PASS | `XuguProjectionIdentifierExtractor` · SHA `353ad82` |
| BindEngine | `infra-binder-xugu` | P1 | PASS | `XuguSQLBindEngine` (Oracle-parity stub, `Optional.empty()`) · SHA `43887fe` |
| Route DAL | `infra-route-xugu` | P1 | PASS | `XuguDALStatementBroadcastRouteDecider` · SHA `9b3b2bd` |
| Rewrite | `infra-rewrite-xugu` | P1 | PASS | `XuguToBeRemovedSegmentsProvider` · SHA `04c8651` |
| Federation connection config | `sql-federation-xugu` | P1 | PASS | `XuguSQLFederationConnectionConfigBuilder` · SHA `c78651a` |
| Federation FunctionRegister / ColumnTypeConverter | `sql-federation-xugu` | P2 | DEFER | deferred: not blocking sharding JDBC path; config builder only for M3 |

---

## G. Feature

| Capability | Pri | Status | Evidence |
|---|---|---|---|
| `PaginationDecoratorMergedResultBuilder` | P0 | PASS | `XuguPaginationDecoratorMergedResultBuilder` (LIMIT) · SHA `034e367`; IT SHA `2a0294f` |
| `DialectShardingDALResultMerger` | P2 | DEFER | deferred: no SHOW-class DAL product need for XuGu NONE sharding path |
| Encrypt / Readwrite dialect SPI | — | PASS | N/A — no independent dialect SPI; follows binder/parser |

---

## H. JDBC periphery

| Capability | Pri | Status | Evidence |
|---|---|---|---|
| `SQLDialectExceptionMapper` | P2 | DEFER | deferred: not blocking sharding JDBC path; driver SQLState/message sufficient for current IT |
| XA `XAConnectionWrapper` | P2 | DEFER | deferred: no confirmed XuGu XA driver class; forbid inventing Oracle XA names (`XuguDatabaseMetaData` keeps `xaDriverClassNames` empty) |
| Savepoint SQL provider | P2 | DEFER | deferred: not required for sharding JDBC CRUD/pagination acceptance |

---

## Summary

| Priority | PASS | DEFER |
|---|---|---|
| P0 | all required items | — |
| P1 | System DB, Kernel tables, DefaultQueryProps, BindEngine, Route, Rewrite, Federation config | PrivilegeChecker only |
| P2 | — | ExceptionMapper, XA, Savepoint, Sharding DAL merge, Federation Function/Converter, parser extensions |

**M3 exit rule:** P0+P1 green (or DEFER with reason where SS does not hard-require) · every P2 has deferred reason → satisfied.
