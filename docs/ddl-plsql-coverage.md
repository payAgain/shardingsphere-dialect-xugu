# Cold DDL + PL/SQL coverage (G-006 Q-04)

> Lab: XuGu `compatiblemode=NONE` · Version frozen `5.5.3-xugu.2`  
> IT: `mvn test -Pddl-plsql` · Class: `DdlPlsqlCoverageIT`  
> Observed: **Supported=36 · DEFER=4** (2026-07-20)

## Scope

Cold DDL and PL/SQL **object surface** (CREATE / ALTER / DROP / CALL) for:

TABLE · INDEX · VIEW · SEQUENCE · PROCEDURE · FUNCTION · TRIGGER · PACKAGE

This is **not** a claim of full PL/SQL language coverage (cursors, packages with complex bodies, autonomous transactions, etc.).

## Supported inventory

| Family | Operations evidenced | Channels | Notes |
|---|---|---|---|
| **TABLE** | CREATE / ALTER ADD / ALTER MODIFY / DROP | parse · native and/or SS | CREATE via SS so binder metadata sees the table |
| **INDEX** | CREATE / ALTER RENAME / DROP `table.index` | parse · native · SS | XuGu requires **qualified** `DROP INDEX t.idx` |
| **VIEW** | CREATE / ALTER RECOMPILE / DROP | parse · native · SS | XuGu uses **RECOMPILE** (not Oracle `COMPILE`) |
| **SEQUENCE** | CREATE / ALTER / DROP | parse · native · SS | |
| **PROCEDURE** | CREATE / ALTER RECOMPILE / CALL / DROP | parse · native · SS | `CALL name` / `CALL name()` |
| **FUNCTION** | CREATE / ALTER RECOMPILE / SELECT f() / DROP | parse · native · SS | |
| **TRIGGER** | CREATE / ALTER ENABLE\|DISABLE / DROP | parse · native · SS | |
| **PACKAGE** | CREATE / CREATE BODY / ALTER RECOMPILE / DROP | parse · native · SS | Empty package rejected by XuGu (see DEFER) |

SS smoke cases `S01`–`S08` create+drop each family through ShardingSphere JDBC (SINGLE DS).

## DEFER (lab-evidenced)

| ID | Statement | Evidence |
|---|---|---|
| **X01** | `CREATE PACKAGE … AS END` (empty) | XuGu syntax error `unexpected _END` |
| **X02** | `DROP INDEX idx` (unqualified) | XuGu E5035 — must use `DROP INDEX table.idx` |
| **X03** | `ALTER INDEX … REBUILD` | XuGu syntax error `unexpected REBUILD` |
| **X04** | `ALTER VIEW … COMPILE` | XuGu expects **RECOMPILE**, not `COMPILE` |

## Parser expansions (minimal)

| Change | Why |
|---|---|
| `CREATE PACKAGE` / `PACKAGE BODY` grammar + visitor | Lab accepts; SS path needs parse |
| Route package DDL via `createMacro` → `SQLVisitorRule.CREATE_MACRO` | SS 5.5.3 core has `DROP`/`ALTER_PACKAGE` but **no** `CREATE_PACKAGE` enum |
| `CALL procedureName [(args)]` | Previous rule was bare `CALL` |
| `RECOMPILE` keyword + ALTER VIEW/PROCEDURE/FUNCTION/PACKAGE | XuGu dialect (lab) |
| Statement types `XuguCreatePackageStatement` / `XuguCreatePackageBodyStatement` | Visitor products |

## Remaining gaps (honest)

- Full PL/SQL procedural language (loops, cursors, exception handlers beyond NULL body) — **not** claimed
- Oracle-only ALTER forms (`COMPILE` without RECOMPILE, `ALTER INDEX REBUILD`) — DEFER per lab
- Unqualified `DROP INDEX` — DEFER per lab (use `table.index`)
- Empty PACKAGE specification — DEFER per lab
- PrivilegeChecker / SHOW DAL — still DEFER (unchanged; not Q-04)

## How to run

```text
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd test "-Pddl-plsql"
```

Requires reachable lab from `tests-it/src/test/resources/it-xugu.properties`.
