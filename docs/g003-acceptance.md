# G-003 Acceptance — DEFER clearance + Baseline IT

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**HEAD:** `1502782`  
**Spec:** `sharding/docs/superpowers/specs/2026-07-20-xugu-baseline-and-defer-clearance-design.md`

## Track1 — DEFER clearance

| Item | Result | Evidence |
|---|---|---|
| Savepoint provider | PASS | `transaction-xugu` · SHA `c13a062` |
| XA wrapper + MetaData XA class | PASS | `com.xugu.xa.XADatasourceImp` / `XAConnectionImp` · SHA `c13a062` |
| SQLDialectExceptionMapper | PASS | `database-exception-xugu` · SHA `c13a062` |
| Federation FunctionRegister / ColumnTypeConverter | PASS | SHA `c13a062` |
| PrivilegeChecker | DEFER | privilege model unmapped (documented in parity-matrix) |
| DAL ResultMerger | DEFER | no SHOW surface in NONE (documented) |
| Full PL/SQL parser | DEFER | baseline-driven only (LIMIT visitor SHA `cced5c7`) |

## Track2 — Baseline IT (live 192.168.2.239)

| ID | Class | Result |
|---|---|---|
| B1 | OrderDbTableShardingIT | PASSED |
| B2 | ReadwriteSplittingIT | PASSED |
| B3 | LocalTransactionSavepointIT | PASSED |
| B4 | BatchInsertIT | PASSED |
| B5 | PaginationListIT | PASSED |
| B6 | EncryptColumnIT | PASSED |
| B7 | XATransactionIT | PASSED |

Command: `mvn -pl tests-it -am test "-Pbaseline"` → Tests run: 7, Failures: 0, Skipped: 0

Catalog: `docs/baseline-catalog.md`

## Unit suite

`mvn -q test` → BUILD SUCCESS (IT excluded by default)

## G-003 exit

Track1 XuGu-native SPIs landed; remaining DEFERs justified; B1–B7 baseline green → **ACCEPT**. Stop before Ship.
