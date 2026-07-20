# Env2 Baseline Result (G-004 P1-3)

> Repo: `shardingsphere-dialect-xugu` · Date: 2026-07-20  
> Related: [baseline-catalog.md](baseline-catalog.md) · [support-matrix.md](support-matrix.md)

## What was used as "env2"

| Item | Value |
|---|---|
| Host / port | **Same lab instance** `192.168.2.239:5138` (identical to primary) |
| Admin URL | `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8` |
| Credentials | `SYSDBA` / `SYSDBA` |
| Properties file | `tests-it/src/test/resources/it-xugu-env2.properties` |
| Maven profiles | `-Penv2` or `-Pit-xugu-env2` (sets `-Dit.xugu.properties=it-xugu-env2.properties`) |
| Fixture | `BaselineSupport` creates DATABASE targets via `CREATE DATABASE` then rewrites JDBC URLs |

### Alternate DATABASE namespace (independent of primary lab DBs)

| Role | Primary (`it-xugu.properties`) | Env2 (`it-xugu-env2.properties`) |
|---|---|---|
| sharding ds0 | `shard_ds0` | `env2_shard_ds0` |
| sharding ds1 | `shard_ds1` | `env2_shard_ds1` |
| readwrite write | `baseline_write` | `env2_baseline_write` |
| readwrite read0 | `baseline_read0` | `env2_baseline_read0` |
| readwrite read1 | `baseline_read1` | `env2_baseline_read1` |

Provisioning: `BaselineSupport.ensureDatabases` with SYSDBA successfully created / connected to the env2 DATABASE names on the same instance (no second machine).

## Suite run

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Penv2" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Profile includes: `**/baseline/**/*IT.java` (B1–B7 full catalog, not a reduced smoke subset)
- Observed JDBC paths in logs: `/env2_shard_ds0`, `/env2_shard_ds1`, `/env2_baseline_write`, `/env2_baseline_read0`, `/env2_baseline_read1`

## Pass / fail counts

| Metric | Count |
|---|---|
| Tests run | **22** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Result | **BUILD SUCCESS** |

Per class (all Failures/Errors/Skipped = 0):

| Class | Tests |
|---|---|
| `BatchInsertIT` | 3 |
| `EncryptColumnIT` | 3 |
| `LocalTransactionSavepointIT` | 3 |
| `OrderDbTableShardingIT` | 3 |
| `PaginationListIT` | 3 |
| `ReadwriteSplittingIT` | 4 |
| `XATransactionIT` | 3 |
| **Total** | **22** |

Status for G-004 P1-3: **PASS** (not `BLOCKED_ENV`).

## Explicit limitations

1. **Same host only.** Env2 is an alternate **DATABASE namespace** on the **same** XuGu instance (`192.168.2.239:5138`). It is **not** a second machine, second site, or independent OS/JVM stack.
2. **Not multi-site / multi-AZ.** No network partition, geographic failover, or cross-datacenter latency was exercised.
3. **Not a heterogeneous second environment.** Same XuGu version, same SYSDBA, same `compatiblemode=NONE`, same driver.
4. **Weak substitute for “environment diversity.”** Useful to prove fixtures and baseline SQL work against a clean parallel namespace without colliding with primary lab DBs; **does not** substitute for a real staging/production twin.
5. **Shared physical resources.** CPU, memory, disk, and connection limits are shared with the primary lab namespace; isolation is logical (DATABASE) only.
6. **Out of Goal:** multi-machine topology remains **OUT OF SCOPE** for G-004 (see design: P1-3 human decision).

## How to re-run

```powershell
# Alias profiles are equivalent:
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Penv2" "-Dsurefire.failIfNoSpecifiedTests=false"
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Pit-xugu-env2" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Prerequisite: modules installed to local Maven repo (`mvn clean install -DskipTests` if dialect jars are stale).
