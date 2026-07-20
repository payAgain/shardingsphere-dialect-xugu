# Load + Fault Injection Evidence (G-004 P1-2)

> Repo: `shardingsphere-dialect-xugu` · Lab: `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8` · Date: 2026-07-20  
> Related: [baseline-catalog.md](baseline-catalog.md) · [xa-recovery-evidence.md](xa-recovery-evidence.md)

## Honesty preamble

This is a **short-run smoke**, not a capacity benchmark and not Gatling/JMeter. Numbers are wall-clock observations from one lab host on one day. They do **not** prove production SLOs, steady-state saturation, or XuGu server-side kill/recovery.

| Path | What it proves | What it does **not** prove |
|---|---|---|
| CRUD + pagination load | Multi-thread ShardingSphere JDBC CRUD/LIMIT survives a fixed ops burst | Sustained throughput, p99 under hours of load |
| Pool exhaustion | Tiny Hikari (`max=2`) rejects acquire when slots are held | SS YAML `maximumPoolSize` pin behavior under logical connections |
| Conn kill mid-flight | Client `abort`/`close` on an SS connection; fresh borrows recover | Guaranteed fail-fast of in-flight ops on the killed handle |

---

## How to run

```powershell
# From repo root (quote -D/-P under PowerShell):
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am install "-Dmaven.test.skip=true"
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it test "-Pperf-fault" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Class: `com.xugudb.shardingsphere.it.perf.LoadAndFaultInjectionIT`

| `#` | Method | Intent |
|---|---|---|
| 1 | `crudPaginationLoadSmoke` | 24 threads × 20 ops CRUD+`LIMIT` via `baseline/baseline-sharding-db.yaml` |
| 2 | `poolExhaustionUnderLoad` | Raw Hikari `maximumPoolSize=2`, hold 2, 32 contenders expect timeout |
| 3 | `connectionKillMidFlight` | Client `abort`/`close` mid-flight on SS connection; 8 post-kill borrows |

Unreachable lab → JUnit Assumption **SKIP** (not PASS).

Optional YAML (SS tiny-pool experiment, not the primary exhaustion path): `tests-it/src/test/resources/perf/perf-sharding-tiny-pool.yaml`.

---

## Observed results (lab 2026-07-20)

### Run metadata

- Host: `192.168.2.239:5138` / `SYSDBA` / `compatiblemode=NONE`
- Command: `mvn -pl tests-it test "-Pperf-fault" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=LoadAndFaultInjectionIT"`
- Surefire: **Tests run: 3, Failures: 0, Errors: 0, Skipped: 0** · **BUILD SUCCESS**
- Wall clock for suite ≈ **10.6 s**

### Key stdout

```text
[P1-2 load] threads=24 ops=480 ok=480 err=0 elapsedMs=1177 opsPerSec=407.8 latAvgMs=51.3 latMaxMs=607.9 latMinMs=9.5 firstError=none
[P1-2 pool-exhaust] threads=32 maxPool=2 held=2 acquired=0 timeoutOrFail=32 elapsedMs=819 sampleFail=SQLTransientConnectionException: p12-tiny - Connection is not available, request timed out after 810ms.
[P1-2 conn-kill] killOutcome=CLOSED_OK afterAbortFail=SQLFeatureNotSupportedException workerOutcome=SURVIVED_AFTER_KILL rows=5 postKillOk=8 postKillErr=0
```

Confirmation re-run (same day): load ≈ **364.5 ops/s** (avg ~59 ms / max ~744 ms); pool-exhaust again **32/32** timeouts (~809 ms); conn-kill identical survival + **8/8** recovery.
### Summary table

| Scenario | Throughput / pressure | Errors | Latency (rough) | Survived | Failed | Notes |
|---|---|---|---|---|---|---|
| Load smoke | **~365–408 ops/s** (480 ops / ~1.2–1.3 s) | **0 / 480** | avg **~51–59 ms**, max **~608–744 ms**, min **~10 ms** | All CRUD+LIMIT cycles | — | ShardingSphere JDBC dual-DB shard |
| Pool exhaust | 32 threads vs maxPool=2 | **32 / 32** acquire timeouts | timeout ≈ **800–810 ms** | Holders (2) | Contenders (32) | Raw Hikari on `jdbc.url.ds0` |
| Conn kill | N/A (single flight) | Worker **survived** close | — | Post-kill borrows **8/8** | `Connection.abort` unsupported | Close did **not** fail subsequent ops on same logical handle |

### Earlier negative observation (SS tiny-pool YAML)

Holding 4 ShardingSphere logical connections against `perf-sharding-tiny-pool.yaml` (`maximumPoolSize: 2` per DS) still allowed 32 concurrent `LIMIT` queries with **0 timeouts** (`held=4 acquired=36 timeoutOrFail=0`). Conclusion: **do not claim** SS logical-connection hold equals physical Hikari exhaustion on this lab path; the proven exhaustion path is **raw Hikari**.

---

## Interpretation

- **Load:** Short multi-thread CRUD + pagination against lab XuGu via baseline sharding YAML is healthy at ~400 ops/s with zero errors in this burst. Max latency (~0.6 s) shows occasional spikes; treat as smoke only.
- **Pool exhaustion:** Proven at the Hikari layer used by baseline configs. Contenders fail with `SQLTransientConnectionException` / connection timeout; holders keep slots. This is the required fault-injection evidence.
- **Conn kill:** XuGu/JDBC `abort` is unsupported (`SQLFeatureNotSupportedException`); `close()` succeeds, but the worker still completed SELECT/INSERT (`SURVIVED_AFTER_KILL`). Fresh SS borrows recovered fully. Fail-fast after client close is **NOT proven**.

---

## Artifacts

- IT: `tests-it/src/test/java/com/xugudb/shardingsphere/it/perf/LoadAndFaultInjectionIT.java`
- Tiny-pool YAML (experimental): `tests-it/src/test/resources/perf/perf-sharding-tiny-pool.yaml`
- Maven profile: `-Pperf-fault` (parent + `tests-it`)
