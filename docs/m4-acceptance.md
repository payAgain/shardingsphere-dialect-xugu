# M4 Acceptance Gate (G-002 — release `5.5.3-xugu.1` + quick-start)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Goal:** G-002 XuGu native dialect M4 (version cut, docs, local consumer smoke)  
**Release version:** **`5.5.3-xugu.1`** (no SNAPSHOT)  
**Status:** **user-ready local release; no remote push**

## Version commits

| Task | Commit message | SHA |
|---|---|---|
| M4-1 | `chore: release version 5.5.3-xugu.1` | `f169009` |
| M4-2 | `docs: quick-start and README for 5.5.3-xugu.1` | `48d8a2b` |
| M4-3 | `docs: M4 acceptance — user-ready release` | this commit |

## Verification commands

```powershell
# Local install (all modules → ~/.m2)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install

# Dual-DS sharding smoke (PowerShell: quote -D args)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd `
  -pl tests-it -am test "-Pit-xugu" `
  "-Dtest=ShardingCrudIT" `
  "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Evidence

| Check | Result | Notes |
|---|---|---|
| Parent + all module versions | **`5.5.3-xugu.1`** | 11 POMs; M4-1 `mvn -q clean test` BUILD SUCCESS |
| `mvn -q clean install` | **PASS** | BUILD SUCCESS at M4-3 smoke (2026-07-20) |
| `ShardingCrudIT` (`-Pit-xugu`) | **PASS** | Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 · host `192.168.2.239:5138` · `compatiblemode=NONE` · dual DS `shard_ds0`/`shard_ds1` |
| Quick-start + README | Present | [`docs/quick-start.md`](quick-start.md), root [`README.md`](../README.md), [`docs/examples/sharding-two-ds.yaml`](examples/sharding-two-ds.yaml) |

### ShardingCrudIT routing (observed)

- INSERT `user_id=1` → `ds_1`; `user_id=2` → `ds_0`
- SELECT by id broadcast to both DS
- `SELECT * FROM t_order LIMIT 5` → both DS with native `LIMIT`

## Consumer coordinates (local `.m2`)

```xml
<dependency>
  <groupId>com.xugudb.shardingsphere</groupId>
  <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
  <version>5.5.3-xugu.1</version>
</dependency>
<!-- plus org.apache.shardingsphere:shardingsphere-jdbc:5.5.3 and com.xugudb:xugu-jdbc:12.3.6 -->
```

## Scope reminder

- JDBC dialect only; **`compatiblemode=NONE` only**; no Proxy; no MySQL trunk
- P2 DEFERs remain documented in [`parity-matrix.md`](parity-matrix.md)
- **No `git push` / remote publish** performed for this gate — install is local-only

## M4 exit

Version cut + quick-start/README + local `mvn clean install` + live `ShardingCrudIT` PASS → M4 gate **PASS**. Local release is user-ready.
