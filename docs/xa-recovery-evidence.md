# XA Recovery Evidence (G-004 P1-1)

> Repo: `shardingsphere-dialect-xugu` · Lab: `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8` · Date: 2026-07-20  
> Related: [support-matrix.md](support-matrix.md) · [baseline-catalog.md](baseline-catalog.md) · B7 `XATransactionIT`

## Honesty preamble

This document records **observed** client/TM-side failure paths on a **same-host lab**.  
It does **not** claim financial-grade XA crash recovery, Atomikos log replay after TM death, multi-RM heuristic resolution, or XuGu server kill/recovery.

| Strength | Meaning |
|---|---|
| **Shallow** | Application/thread abort or connection close before durable 2PC complete; rows absent mostly because commit never succeeded |
| **Medium** | Failure injected at prepare/commit boundary; residual state / `recover()` probed |
| **Strong** | TM JVM crash **after prepare** + restart recovers/commits/rolls back via TM log + RM `recover()` — **not proven here** |

**Overall verdict for P1-1 on this lab run:** evidence is **shallow → weak-medium**. Two of three required failure modes were exercised with clear observations; timeout is a documented **GAP**. Strong Atomikos/XuGu heuristic recovery remains **NOT proven**.

---

## How to run

### A. IT suite (`-Pxa-recovery`)

```powershell
# Prefer install deps without recompiling broken upstream test sources if needed:
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am install "-Dmaven.test.skip=true"
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Pxa-recovery" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Class: `com.xugudb.shardingsphere.it.xa.XARecoveryEvidenceIT`

| `#` | Method | Failure mode | Strength target |
|---|---|---|---|
| 1 | `interruptMidXaBeforeCommitLeavesNoRows` | Interrupt worker mid-XA (after INSERT, before commit) on ShardingSphere+Atomikos | Shallow |
| 2 | `xaResourceTimeoutPathIsObservable` | Raw `XAResource.setTransactionTimeout(2)` then sleep past window | Shallow / Medium if RM aborts |
| 3 | `connectionKillDuringPrepareLeavesRecoverableOrCleanState` | Close JDBC + `XAConnection` then `prepare`/`commit` | Medium |

Unreachable lab → JUnit Assumption **SKIP** (not PASS).

### B. Optional client JVM kill script

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-kill-client.ps1
```

Kills **client JVM only** after `READY_FOR_KILL` (XuGu server untouched). Probe prints `PROBE_COUNT` / `PROBE_RECOVER`.

---

## Observed results (lab 2026-07-20)

| Path | Result | Observation summary | What recovered | Heuristic / leftover | NOT proven |
|---|---|---|---|---|---|
| Interrupt mid-XA | **PASS (shallow)** | `InterruptedException` on await; Atomikos `XAResource.rollback` on both shards; `counts ds0=0 ds1=0` | App/TM rollback of active branch | None | Crash recovery / prepare-phase interrupt |
| XA timeout | **GAP (observable)** | `setTransactionTimeout(2)` → `setAccepted=false`, `timeoutSec=0`; still `COMMITTED_DESPITE_TIMEOUT vote=0`; `remainingRows=1`; `recover=[]` | Nothing — timeout ignored | Durable row until IT cleanup | Timeout-enforced abort / recovery |
| Conn kill @ prepare | **PASS (weak-medium)** | Close conn+XAConn then prepare: `PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7` (`XAER_RMFAIL`); `remainingRows=0`; `recoverCount=0` | No durable row after failed commit; RM fail on commit after close | No in-doubt Xid via `recover()` | Heuristic complete/forget; prepared-then-crash recovery |
| Client JVM kill (script) | **PASS (shallow)** | `READY_FOR_KILL` then `Stop-Process`; `PROBE_COUNT=0 PROBE_RECOVER=0` | Kill before prepare/commit → no durable row (disconnect abort) | None visible | Kill **after** prepare + TM restart recovery |

### Run metadata

- Host: `192.168.2.239:5138` / `SYSDBA` / `compatiblemode=NONE`
- IT: `mvn -pl tests-it test "-Pxa-recovery" "-Dsurefire.failIfNoSpecifiedTests=false"` → **Tests run: 3, Failures: 0**
- Script log: `tests-it/logs/xa-recovery-kill-client.log` (+ probe log)
- Key stdout excerpts:

```text
[P1-1 interrupt] interruptedFlag=false workerError=InterruptedException: null counts ds0=0 ds1=0
[P1-1 timeout] outcome=COMMITTED_DESPITE_TIMEOUT vote=0 setAccepted=false timeoutSec=0 remainingRows=1 recover=[]
[P1-1 timeout] GAP: RM did not abort after setTransactionTimeout(2); ... timeout recovery NOT proven
[P1-1 conn-kill] prepareOutcome=PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7 remainingRows=0 recoverCount=0 recover=[]
PROBE_COUNT=0 PROBE_RECOVER=0
```

---

## Interpretation

- **Interrupt → 0 rows** proves ShardingSphere/Atomikos rollback wiring on thread interrupt **before commit**. Shallow only.
- **Timeout commits anyway** with `setAccepted=false` / `timeoutSec=0`: XuGu `XAResourceImp` does **not** honor `setTransactionTimeout` on this driver build. Do **not** claim timeout recovery.
- **Close-then-prepare** still prepared (`vote=0`) but **commit failed** (`XAER_RMFAIL` / `-7`) and left **0 rows** and **empty recover()**. Suggests branch did not stay in prepared/heuristic state visible to a new connection — medium evidence at best; not strong recovery.
- **JVM kill before prepare** with `PROBE_COUNT=0` is expected disconnect cleanup, **not** proof of prepared-transaction recovery.

---

## Explicitly NOT proven

1. Atomikos TM log replay after JVM death mid-2PC  
2. XuGu `recover()` returning in-doubt Xids that are then `commit`/`rollback`/`forget`  
3. Multi-shard heuristic mixed outcomes  
4. Server-side process kill / RM crash recovery  
5. Production SLA for XA timeout
