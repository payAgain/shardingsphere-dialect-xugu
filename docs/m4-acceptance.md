# M4 Acceptance Gate (G-002 — Release `5.5.3-xugu.1` + Quick Start)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Goal:** G-002 XuGu native dialect M4 (release coordinates + consumer quick-start)  
**HEAD at verification:** `48d8a2b` (+ this acceptance doc commit)

## Verification commands

```powershell
# Unit suite
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q test

# Local release install
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install

# Live dual-DS sharding smoke (NONE mode)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test "-Pit-xugu" "-Dtest=ShardingCrudIT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## M4 checklist

| Item | Evidence | Result |
|---|---|---|
| M4-1 Version `5.5.3-xugu.1` (non-SNAPSHOT) | Parent + all modules `pom.xml` version | PASS |
| M4-2 Quick-start (~30 min) | `docs/quick-start.md` · README points here | PASS |
| M4-3 Example YAML | `docs/examples/sharding-two-ds.yaml` | PASS |
| M4-4 Local `mvn clean install` | Consumer can resolve `shardingsphere-jdbc-dialect-xugu:5.5.3-xugu.1` from `.m2` | PASS (reconfirmed at G-002 close) |
| M4-5 Live sharding smoke | `ShardingCrudIT` re-run 2026-07-20 → Tests run: 1, Failures: 0 · BUILD SUCCESS | PASS |
| M4-6 Unit suite green | `mvn -q test` → BUILD SUCCESS (UNIT_EXIT=0) | PASS |

## Release coordinates

| Artifact | Version |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | `5.5.3-xugu.1` |
| Upstream `org.apache.shardingsphere:shardingsphere-jdbc` | `5.5.3` |
| Driver `com.xugudb:xugu-jdbc` | `12.3.6` (local install) |

## Scope reminder

- JDBC only · `compatiblemode=NONE` only · no MySQL trunk · no Proxy
- Remote publish / push / PR = Human Ship gate (not part of M4 exit)

## Prior gates

| Gate | Doc | Status |
|---|---|---|
| M0–M1 | `docs/m0-m1-acceptance.md` | PASS |
| M2 | `docs/m2-acceptance.md` | PASS |
| M3 | `docs/m3-acceptance.md` + `docs/parity-matrix.md` | PASS |

## M4 exit

Release version + quick-start + local install path + live `ShardingCrudIT` reconfirmed → M4 gate **PASS**. G-002 complete (stop before Ship).
