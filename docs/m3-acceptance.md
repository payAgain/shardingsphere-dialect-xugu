# M3 Acceptance Gate (G-002 — P1 connector / binder / federation + parity)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Goal:** G-002 XuGu native dialect M3 (system DB, query props, BindEngine stub, federation config, parity matrix)  
**Acceptance docs SHA:** this commit (`docs: M3 parity matrix and acceptance`)

## Verification commands

```powershell
# Unit suite (Surefire excludes *IT by default)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q test

# Optional live IT (not required for M3 exit; covered in M2)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test -Pit-xugu -Dtest=ShardingCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

## M3 checklist

| Task | Item | Evidence | Result |
|---|---|---|---|
| M3-1 | `DialectSystemDatabase` + `DialectKernelSupportedSystemTable` | `XuguSystemDatabase` / `XuguKernelSupportedSystemTable` · SHA `eff8f22` | PASS |
| M3-2 | `DialectDefaultQueryPropertiesProvider` | `XuguDefaultQueryPropertiesProvider` · `compatiblemode=NONE` · SHA `cb7cabd` | PASS |
| M3-3 | `DialectSQLBindEngine` stub | `XuguSQLBindEngine` · SHA `43887fe` | PASS |
| M3-4 | Federation `DialectSQLFederationConnectionConfigBuilder` | `XuguSQLFederationConnectionConfigBuilder` · SHA `c78651a` | PASS |
| M3-5 | Privilege checker decision | **DEFER** — XuGu privilege model unmapped; JDBC sharding path does not require SPI; Oracle also lacks checker (no fake impl) | PASS (documented) |
| M3-5 | `docs/parity-matrix.md` | Full §3.2 P0/P1/P2 table with PASS/DEFER + evidence | PASS |
| M3-5 | `mvn -q test` | BUILD SUCCESS at acceptance | PASS |

## Privilege checker decision

- **Decision:** DEFER — do not implement `DialectDatabasePrivilegeChecker`.
- **Rationale:** XuGu privilege model is not mapped to SS checker API; standard JDBC sharding CRUD does not hard-require the SPI. Upstream Oracle connector likewise has no privilege checker registration.
- **Constraint honored:** no invented no-op checker.

## P2 deferred (must-document)

| Item | Reason |
|---|---|
| `SQLDialectExceptionMapper` | Not blocking sharding JDBC path |
| XA `XAConnectionWrapper` | No confirmed XuGu XA class; must not copy Oracle XA names |
| Savepoint SQL provider | Not required for current acceptance |
| `DialectShardingDALResultMerger` | No SHOW-class DAL product need |
| Federation FunctionRegister / ColumnTypeConverter | Config-only federation for M3; not blocking sharding JDBC |

Full matrix: [`docs/parity-matrix.md`](parity-matrix.md).

## M3 exit

P0 + implemented P1 SPIs green; PrivilegeChecker DEFER documented; all P2 items have deferred reasons; unit `mvn -q test` BUILD SUCCESS → M3 gate **PASS**. M4 (release `5.5.3-xugu.1` + quick-start) may proceed.
