# XA Recovery Evidence (G-005 T2 / G-004 P1-1)

> Repo: `shardingsphere-dialect-xugu` · Lab: `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8` · Date: 2026-07-20  
> Related: [support-matrix.md](support-matrix.md) · [baseline-catalog.md](baseline-catalog.md) · B7 `XATransactionIT`

## Honesty preamble

This document records **observed** client/TM-side failure paths on a **same-host lab**.  
It does **not** claim financial-grade XA crash recovery, Atomikos log replay after TM death, multi-RM heuristic resolution, or XuGu server kill/recovery.

| Strength | Meaning |
|---|---|
| **Shallow** | Application/thread abort or connection close before durable 2PC complete; rows absent mostly because commit never succeeded |
| **Medium** | Failure injected **at/after prepare**; residual state / `recover()` probed; disposition classified (clean / in-doubt / durable-row) |
| **Strong** | TM JVM crash **after prepare** + restart recovers/commits/rolls back via TM log + RM `recover()` heuristic complete — **not proven here** |

**Overall verdict (G-005 T2 + G-006 Q-02):** evidence is **medium** for prepare-then-kill (IT close + JVM kill both leave `CLEAN_ROLLBACK_OR_ABORT`). Timeout gap **CLOSED_AS_DEFER** (driver stub ignores `setTransactionTimeout`; no wrapper alternative). Strong Atomikos/XuGu heuristic recovery remains **NOT proven**.

---

## How to run

### A. IT suite (`-Pxa-recovery`)

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am install "-Dmaven.test.skip=true"
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Pxa-recovery" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Class: `com.xugudb.shardingsphere.it.xa.XARecoveryEvidenceIT`

| `#` | Method | Failure mode | Strength target |
|---|---|---|---|
| 1 | `interruptMidXaBeforeCommitLeavesNoRows` | Interrupt worker mid-XA (after INSERT, before commit) on ShardingSphere+Atomikos | Shallow |
| 2 | `xaResourceTimeoutPathIsObservable` | Raw `XAResource.setTransactionTimeout(2)` then sleep past window; wrapper has no alt API | **CLOSED_AS_DEFER** if ignored |
| 3 | `connectionKillDuringPrepareLeavesRecoverableOrCleanState` | Close JDBC + `XAConnection` **then** `prepare`/`commit` | Weak-medium |
| 4 | `killAfterPrepareLeavesRecoverableOrCleanState` | `prepare` OK → close conn/XAConn → commit attempt → `recover()` + rows | Medium |

Unreachable lab → JUnit Assumption **SKIP** (not PASS).

### B. Client JVM kill **after prepare**

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-kill-client.ps1
```

Flow: start → INSERT → end → **prepare** → `READY_FOR_KILL phase=AFTER_PREPARE` → `Stop-Process` (XuGu server untouched) → probe `PROBE_COUNT` / `PROBE_RECOVER` / `disposition`.

---

## Observed results (lab 2026-07-20, G-005 T2)

| Path | Result | Observation summary | What recovered | Heuristic / leftover | NOT proven |
|---|---|---|---|---|---|
| Interrupt mid-XA | **PASS (shallow)** | `InterruptedException`; Atomikos `XAResource.rollback` both shards; `counts ds0=0 ds1=0` | App/TM rollback of active branch | None | Crash recovery / prepare-phase interrupt |
| XA timeout | **CLOSED_AS_DEFER** | Re-probe G-006 Q-02: `XAResourceImp` bytecode stub (`setTransactionTimeout`→`false`, `getTransactionTimeout`→`0`); `XuguXAConnectionWrapper` has **no** timeout API. Lab: `COMMITTED_DESPITE_TIMEOUT vote=0`; `remainingRows=1`; `recover=[]` | Nothing — timeout ignored | Durable row until IT cleanup | RM timeout-enforced abort / recovery |
| Conn close before prepare | **PASS (weak-medium)** | Close then prepare: `PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7`; `remainingRows=0`; `recoverCount=0` | No durable row after failed commit | No in-doubt Xid via `recover()` | Heuristic complete/forget |
| Kill/close **after prepare** (IT) | **PASS (medium)** | `PREPARE_OK vote=0`; post-close `COMMIT_XAEX=-7; ROLLBACK_XAEX=-7`; `remainingRows=0`; `recoverCount=0`; `disposition=CLEAN_ROLLBACK_OR_ABORT` | RM cleaned branch after TM disconnect post-prepare (no durable row) | No in-doubt via `recover()` | Heuristic commit/rollback/forget; TM-log replay |
| Client JVM kill **after prepare** (script) | **PASS (medium)** | `PREPARED vote=0` → `READY_FOR_KILL phase=AFTER_PREPARE` → kill; `PROBE_COUNT=0 PROBE_RECOVER=0 disposition=CLEAN_ROLLBACK_OR_ABORT` | Disconnect after prepare → no durable row / empty recover | None visible | Strong TM restart recovery from Atomikos log |

### Timeout disposition (**CLOSED_AS_DEFER** — ops workaround)

**G-006 Q-02 re-probe (2026-07-20):** XuGu JDBC `12.3.6` `com.xugu.xa.XAResourceImp` implements `setTransactionTimeout` / `getTransactionTimeout` as **no-op stubs** (always `false` / `0`). `XuguXAConnectionWrapper` only constructs `XAConnectionImp` — **no alternate timeout surface**. Lab IT still commits after sleeping past a 2s window (`COMMITTED_DESPITE_TIMEOUT`). Gap closed as **CLOSED_AS_DEFER** — do **not** claim RM XA timeout abort works.

**Ops workaround (application / TM-level):** enforce transaction / statement deadlines in the application or TM **before** entering 2PC (cancel / rollback active branch; optional `Statement.setQueryTimeout` for statement-bound work). Do **not** rely on RM `XAResource.setTransactionTimeout`. Documented also in [support-matrix.md](support-matrix.md).

### Run metadata

- Host: `192.168.2.239:5138` / `SYSDBA` / `compatiblemode=NONE`
- IT: `mvn -pl tests-it test "-Pxa-recovery"` → **Tests run: 4, Failures: 0** (re-run under G-006 Q-02)
- Script log: `tests-it/logs/xa-recovery-kill-client.log` (+ probe log)
- Key stdout excerpts:

```text
[T2 interrupt] interruptedFlag=false workerError=InterruptedException: null counts ds0=0 ds1=0
[Q-02 timeout] xaResClass=com.xugu.xa.XAResourceImp wrapperAlt=none (XuguXAConnectionWrapper has no timeout API; same XAResourceImp)
[Q-02 timeout] outcome=COMMITTED_DESPITE_TIMEOUT vote=0 setAccepted=false timeoutSec=0 remainingRows=1 recover=[]
[Q-02 timeout] disposition=CLOSED_AS_DEFER setAccepted=false timeoutSec=0
[Q-02 timeout] CLOSED_AS_DEFER: RM ignored setTransactionTimeout(2); ops workaround = application/TM-level timeout / cancel before 2PC; do NOT claim RM XA timeout abort works
[T2 conn-kill-before-prepare] prepareOutcome=PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7 remainingRows=0 recoverCount=0 recover=[]
[T2 kill-after-prepare] PREPARE_OK vote=0 postClose=COMMIT_XAEX=-7; ROLLBACK_XAEX=-7 remainingRows=0 recoverCount=0 recover=[] disposition=CLEAN_ROLLBACK_OR_ABORT
PREPARED vote=0 ...
READY_FOR_KILL phase=AFTER_PREPARE ...
PROBE_COUNT=0 PROBE_RECOVER=0 detail=startScan=0 endScan=0 disposition=CLEAN_ROLLBACK_OR_ABORT
```

---

## Interpretation

- **Interrupt → 0 rows** proves ShardingSphere/Atomikos rollback wiring on thread interrupt **before commit**. Shallow only.
- **Timeout commits anyway** (`setAccepted=false` / `timeoutSec=0`): **CLOSED_AS_DEFER** — use app/TM-level timeout; do not claim RM XA timeout abort.
- **Close-then-prepare** still prepared (`vote=0`) but commit failed (`XAER_RMFAIL` / `-7`) with **0 rows** and empty `recover()`.
- **Prepare-then-kill/close** (IT + JVM script) is the upgraded T2 path: prepare succeeds (`vote=0`), then TM/client death; probe shows **CLEAN_ROLLBACK_OR_ABORT** (no durable row, no in-doubt Xid). This is **medium** evidence that the RM does not leave a visible prepared residue after client death — **not** strong proof of TM-log-driven recovery or heuristic complete/forget.

---

## Explicitly NOT proven (still GAP)

1. Atomikos TM log replay after JVM death mid-2PC with restart → commit/rollback of prepared branches  
2. XuGu `recover()` returning in-doubt Xids that are then `commit`/`rollback`/`forget`  
3. Multi-shard heuristic mixed outcomes  
4. Server-side process kill / RM crash recovery  
5. Production SLA for XA RM timeout (CLOSED_AS_DEFER — use app/TM-level timeout)
