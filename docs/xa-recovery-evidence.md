# XA Recovery Evidence (G-005 T2 / G-004 P1-1 / G-006 Q-01/Q-02)

> Repo: `shardingsphere-dialect-xugu` · Lab: `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8` · Date: 2026-07-20  
> Related: [support-matrix.md](support-matrix.md) · [baseline-catalog.md](baseline-catalog.md) · B7 `XATransactionIT`

## Honesty preamble

This document records **observed** client/TM-side failure paths on a **same-host lab**.  
It does **not** claim financial-grade XA crash recovery, Atomikos log replay after TM death, multi-RM heuristic resolution, or XuGu server kill/recovery.

| Strength | Meaning |
|---|---|
| **Shallow** | Application/thread abort or connection close before durable 2PC complete; rows absent mostly because commit never succeeded |
| **Medium** | Failure injected **at/after prepare**; residual state / `recover()` probed; disposition classified (clean / in-doubt / durable-row) |
| **Strong** | TM JVM crash **after prepare** + restart recovers/commits/rolls back via TM log + RM `recover()` heuristic complete |

**Overall verdict (G-006 Q-01):** Strong path attempted and **BLOCKED** — after prepare-then-TM-kill, XuGu `recover()` returns **no in-doubt Xids** and rows=`0` (`CLEAN_ROLLBACK_OR_ABORT`). Cannot demonstrate post-crash `recover()` + heuristic commit/rollback. Medium prepare-then-kill evidence remains valid. Timeout gap **CLOSED_AS_DEFER** (Q-02).

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
| 5 | `strongTmRecoveryAfterPrepareAttempt` | prepare → TM disconnect → `recover()` + heuristic resolve; prints `STRONG_PASS` or `STRONG_BLOCKED` | **Strong** (honest BLOCKED ok) |

Unreachable lab → JUnit Assumption **SKIP** (not PASS).

### B. Client JVM kill **after prepare** (medium)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-kill-client.ps1
```

Flow: start → INSERT → end → **prepare** → `READY_FOR_KILL phase=AFTER_PREPARE` → `Stop-Process` (XuGu server untouched) → probe `PROBE_COUNT` / `PROBE_RECOVER` / `disposition`.

### C. Strong attempt (G-006 Q-01)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-strong.ps1
```

Flow:

1. Atomikos `UserTransactionManager` with preserved log dir `tests-it/logs/xa-strong-tm` (enlist may fail without registered recoverable XA resource → **raw-XA fallback**)
2. `prepare` succeeds → `READY_FOR_KILL phase=AFTER_PREPARE_*` → kill TM/client JVM (server untouched)
3. New JVM: Atomikos restart against same log dir + RM `recover()` + heuristic commit/rollback/forget
4. Prints `STRONG_VERDICT=STRONG_PASS|STRONG_BLOCKED`

**Strong PASS gate:** `recover()` returns ≥1 in-doubt Xid after TM kill **and** heuristic resolve clears the scan. Anything else → **BLOCKED** (not fake PASS).

---

## Observed results (lab 2026-07-20)

| Path | Result | Observation summary | What recovered | Heuristic / leftover | NOT proven |
|---|---|---|---|---|---|
| Interrupt mid-XA | **PASS (shallow)** | `InterruptedException`; Atomikos `XAResource.rollback` both shards; `counts ds0=0 ds1=0` | App/TM rollback of active branch | None | Crash recovery / prepare-phase interrupt |
| XA timeout | **CLOSED_AS_DEFER** | Re-probe G-006 Q-02: `XAResourceImp` bytecode stub (`setTransactionTimeout`→`false`, `getTransactionTimeout`→`0`); `XuguXAConnectionWrapper` has **no** timeout API. Lab: `COMMITTED_DESPITE_TIMEOUT vote=0`; `remainingRows=1`; `recover=[]` | Nothing — timeout ignored | Durable row until IT cleanup | RM timeout-enforced abort / recovery |
| Conn close before prepare | **PASS (weak-medium)** | Close then prepare: `PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7`; `remainingRows=0`; `recoverCount=0` | No durable row after failed commit | No in-doubt Xid via `recover()` | Heuristic complete/forget |
| Kill/close **after prepare** (IT) | **PASS (medium)** | `PREPARE_OK vote=0`; post-close `COMMIT_XAEX=-7; ROLLBACK_XAEX=-7`; `remainingRows=0`; `recoverCount=0`; `disposition=CLEAN_ROLLBACK_OR_ABORT` | RM cleaned branch after TM disconnect post-prepare (no durable row) | No in-doubt via `recover()` | Heuristic commit/rollback/forget; TM-log replay |
| Client JVM kill **after prepare** (script) | **PASS (medium)** | `PREPARED vote=0` → `READY_FOR_KILL phase=AFTER_PREPARE` → kill; `PROBE_COUNT=0 PROBE_RECOVER=0 disposition=CLEAN_ROLLBACK_OR_ABORT` | Disconnect after prepare → no durable row / empty recover | None visible | Strong TM restart recovery from Atomikos log |
| **Strong TM recovery (Q-01)** | **BLOCKED** | IT: `STRONG_VERDICT=STRONG_BLOCKED` `recoverBefore=0` `rows=0`. Script: Atomikos enlist rejected (no recoverable registered resource) → raw-XA prepare `vote=0` → kill → TM restart OK → `RECOVER_BEFORE=0` `PROBE_COUNT=0` → `STRONG_BLOCKED` `NO_IN_DOUBT_AFTER_TM_KILL` | Nothing to recover | None (RM auto-cleaned) | Strong recover()+heuristic; Atomikos TM-log driven completion of prepared branches |

### Timeout disposition (**CLOSED_AS_DEFER** — ops workaround)

**G-006 Q-02 re-probe (2026-07-20):** XuGu JDBC `12.3.6` `com.xugu.xa.XAResourceImp` implements `setTransactionTimeout` / `getTransactionTimeout` as **no-op stubs** (always `false` / `0`). `XuguXAConnectionWrapper` only constructs `XAConnectionImp` — **no alternate timeout surface**. Lab IT still commits after sleeping past a 2s window (`COMMITTED_DESPITE_TIMEOUT`). Gap closed as **CLOSED_AS_DEFER** — do **not** claim RM XA timeout abort works.

**Ops workaround (application / TM-level):** enforce transaction / statement deadlines in the application or TM **before** entering 2PC (cancel / rollback active branch; optional `Statement.setQueryTimeout` for statement-bound work). Do **not** rely on RM `XAResource.setTransactionTimeout`. Documented also in [support-matrix.md](support-matrix.md).

### Strong BLOCKED reason (G-006 Q-01)

1. **RM behavior:** After prepare + TM/client JVM death (or TM-side connection close), XuGu leaves **no** visible prepared residue — `recover()` empty, durable rows `0` (`CLEAN_ROLLBACK_OR_ABORT`). There is nothing for a restarted process to `commit`/`rollback`/`forget`.
2. **Atomikos enlist:** Direct `Transaction.enlistResource` of a wrapped XuGu `XAResource` fails even with `automatic_resource_registration=true` (“no registered resource that can recover the given XAResource”). Strong script falls back to raw XA prepare-hold; Atomikos log dir is preserved but does not contain a recoverable prepared coordinator entry for this probe.
3. Therefore Strong acceptance is **BLOCKED**, not PASS. Medium prepare-then-kill evidence is unchanged.

### Run metadata

- Host: `192.168.2.239:5138` / `SYSDBA` / `compatiblemode=NONE`
- Version frozen: `5.5.3-xugu`
- IT: `mvn -pl tests-it test "-Pxa-recovery"` → **Tests run: 5, Failures: 0** (G-006 Q-01)
- Strong script log: `tests-it/logs/xa-recovery-strong.log` (+ `-recover.log`)
- Medium script log: `tests-it/logs/xa-recovery-kill-client.log` (+ probe log)
- Key stdout excerpts:

```text
[T2 interrupt] interruptedFlag=false workerError=InterruptedException: null counts ds0=0 ds1=0
[Q-02 timeout] xaResClass=com.xugu.xa.XAResourceImp wrapperAlt=none (XuguXAConnectionWrapper has no timeout API; same XAResourceImp)
[Q-02 timeout] outcome=COMMITTED_DESPITE_TIMEOUT vote=0 setAccepted=false timeoutSec=0 remainingRows=1 recover=[]
[Q-02 timeout] disposition=CLOSED_AS_DEFER setAccepted=false timeoutSec=0
[T2 conn-kill-before-prepare] prepareOutcome=PREPARE_OK_AFTER_CLOSE vote=0; COMMIT_XAEX=-7 remainingRows=0 recoverCount=0 recover=[]
[T2 kill-after-prepare] PREPARE_OK vote=0 postClose=COMMIT_XAEX=-7; ROLLBACK_XAEX=-7 remainingRows=0 recoverCount=0 recover=[] disposition=CLEAN_ROLLBACK_OR_ABORT
[Q-01 strong] PREPARE_OK vote=0 recoverBefore=0 recoverAfter=0 remainingRows=0 actions=[] STRONG_VERDICT=STRONG_BLOCKED reason=NO_IN_DOUBT_AFTER_TM_KILL: recover() empty; rows=0; cannot demonstrate Strong recover+commit/rollback path
PREPARED vote=0 ...
READY_FOR_KILL phase=AFTER_PREPARE_RAW_XA ...
TM_RESTART_RESULT=TM_RESTART_OK
RECOVER_BEFORE count=0 xids=[]
PROBE_COUNT_BEFORE_RESOLVE=0 PROBE_COUNT_AFTER_RESOLVE=0
STRONG_VERDICT=STRONG_BLOCKED reason=NO_IN_DOUBT_AFTER_TM_KILL: recover() empty after prepare-then-kill; XuGu left CLEAN_ROLLBACK_OR_ABORT (rows=0); Atomikos TM-log restart did not surface RM in-doubt for commit/rollback
```

---

## Interpretation

- **Interrupt → 0 rows** proves ShardingSphere/Atomikos rollback wiring on thread interrupt **before commit**. Shallow only.
- **Timeout commits anyway** (`setAccepted=false` / `timeoutSec=0`): **CLOSED_AS_DEFER** — use app/TM-level timeout; do not claim RM XA timeout abort.
- **Close-then-prepare** still prepared (`vote=0`) but commit failed (`XAER_RMFAIL` / `-7`) with **0 rows** and empty `recover()`.
- **Prepare-then-kill/close** (IT + JVM script) is the upgraded T2 path: prepare succeeds (`vote=0`), then TM/client death; probe shows **CLEAN_ROLLBACK_OR_ABORT** (no durable row, no in-doubt Xid). This is **medium** evidence that the RM does not leave a visible prepared residue after client death — **not** strong proof of TM-log-driven recovery or heuristic complete/forget.
- **Q-01 Strong:** attempted end-to-end; outcome **BLOCKED** because the post-kill recover/heuristic path has no in-doubt work to do.

---

## Explicitly NOT proven (still GAP / BLOCKED)

1. Atomikos TM log replay after JVM death mid-2PC with restart → commit/rollback of prepared branches  
2. XuGu `recover()` returning in-doubt Xids that are then `commit`/`rollback`/`forget`  
3. Multi-shard heuristic mixed outcomes  
4. Server-side process kill / RM crash recovery  
5. Production SLA for XA RM timeout (CLOSED_AS_DEFER — use app/TM-level timeout)
