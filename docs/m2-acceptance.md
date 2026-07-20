# M2 Acceptance Gate (G-002 partial — Route/Rewrite/Pagination + Sharding IT)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Goal:** G-002 XuGu native dialect M2 (shard Route/Rewrite + LIMIT merge + dual-DS sharding IT)  
**ShardingCrudIT PASS SHA:** this commit (`test(it): verify XuGu sharding CRUD and LIMIT pagination`)

## Verification commands

```powershell
# Unit (Surefire excludes *IT by default)
mvn -q test

# Live dual-DS sharding IT
mvn -pl tests-it -am test -Pit-xugu -Dtest=ShardingCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

## Multi-DB / schema probe (NONE mode)

Host: `192.168.2.239:5138` · User: `SYSDBA` · URL base: `jdbc:xugu://…/SYSTEM?compatiblemode=NONE&charset=UTF8`

| Probe | Result |
|---|---|
| `CREATE DATABASE shard_ds0` / `shard_ds1` | **OK** on first probe; subsequent runs: `E2007` already-exists (treated as usable) |
| Connect `jdbc:xugu://…/shard_ds0` and `…/shard_ds1` | **OK** (`catalog=shard_ds0\|shard_ds1`, `schema=SYSDBA`) |
| Fallback `CREATE SCHEMA SS_SHARD_0/1` + `current_schema=` | Also OK (not needed; DATABASE mode preferred) |

**Chosen shard mode for IT:** `DATABASE` — two JDBC URLs with distinct database names `shard_ds0` / `shard_ds1`.

## Sharding rule

- Logical table: `t_order` → actual `ds_${0..1}.T_ORDER` (XuGu `IdentifierPatternType.UPPER_CASE`)
- Database sharding only: `user_id % 2` → `ds_0` / `ds_1` (INLINE)
- YAML: `tests-it/src/test/resources/sharding-two-ds.yaml`

## ShardingCrudIT evidence

Flow: ensure DBs → CREATE `T_ORDER` on both DS → INSERT `user_id=1` & `user_id=2` → SELECT by `id` → physical count check → `SELECT * FROM t_order LIMIT 5` → DROP.

Observed routing (`sql-show: true`):

| Logic | Actual |
|---|---|
| INSERT `user_id=1` | `ds_1 ::: INSERT INTO T_ORDER …` |
| INSERT `user_id=2` | `ds_0 ::: INSERT INTO T_ORDER …` |
| SELECT by id | fan-out `ds_0` + `ds_1` |
| `SELECT * FROM t_order LIMIT 5` | fan-out both DS with `LIMIT 5` (pagination merge SPI path) |

**Surefire:** `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` · **BUILD SUCCESS**

## M2 checklist

| Item | Evidence | Result |
|---|---|---|
| M2-1 `infra-route-xugu` DAL broadcast stub | Commit `9b3b2bd` · unit tests | PASS |
| M2-2 `infra-rewrite-xugu` empty segments provider | Commit `04c8651` · unit tests | PASS |
| M2-3 `sharding-dialect-xugu` LIMIT merge builder | Commit `034e367` · SPI + `LimitDecoratorMergedResult` | PASS |
| M2-4 `ShardingCrudIT` dual-DS CRUD + LIMIT | This commit · `-Pit-xugu` PASS | PASS |

## M2 exit

Route + Rewrite + Pagination SPI unit green; live `ShardingCrudIT` PASS on DATABASE mode → M2 gate **PASS**. M3 (P1 connector matrix) may proceed.
