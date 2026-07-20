# XuGu × ShardingSphere Production Support Matrix

> **Audience:** consumers evaluating dialect readiness for general production  
> **Release:** 5.5.3-xugu.2 · notes: [RELEASE-NOTES-5.5.3-xugu.2.md](RELEASE-NOTES-5.5.3-xugu.2.md)  
> **Upstream:** Apache ShardingSphere JDBC `5.5.3`  
> **Dialect:** XuGu native JDBC (`getDatabaseType() == "XuGu"`)  
> **Date:** 2026-07-20  
> **Related:** [parity-matrix.md](parity-matrix.md) · [baseline-catalog.md](baseline-catalog.md) · [quick-start.md](quick-start.md) · [pagination-decision.md](pagination-decision.md)

This matrix is the **external** capability whitelist for G-004 hardening. SPI-level PASS/DEFER detail lives in [parity-matrix.md](parity-matrix.md); do not treat this document as a claim of financial-grade XA, multi-site HA, or unlimited SQL coverage.

---

## 1. Environment assumptions (must hold)

| Assumption | Requirement | Notes |
|---|---|---|
| Compatible mode | **`compatiblemode=NONE` only** | Dialect default query props force NONE; other XuGu modes are out of product scope |
| Topology | **Single lab / single-host** simulation | Default IT host `192.168.2.239:5138` (`tests-it/.../it-xugu.properties`) |
| Driver | `com.xugudb:xugu-jdbc` **12.3.6** | Install into local `.m2` before build/consume |
| Runtime surface | **ShardingSphere JDBC** + dialect JAR on classpath | No Proxy module in this product |
| Identifiers | Unquoted → **UPPER_CASE** physical names | Logic tables may be lowercase; physical nodes often `BASELINE_*` / `T_ORDER` style |

If a host is unreachable, baseline ITs **Assumption-skip** (not fail). Skip ≠ production evidence.

---

## 2. Supported (production-candidate under assumptions)

Capabilities below are **in product scope** and have dialect SPI and/or baseline IT evidence. “Supported” here means *allowed for controlled production use within the whitelist*, not that every hardening gap is closed (see §5).

| Area | Status | What is covered | Evidence / caveat |
|---|---|---|---|
| **JDBC dialect SPI** | Supported | `DatabaseType`, connection props, metadata, result-set mapper, system DB, default query props (`compatiblemode=NONE`) | [parity-matrix.md](parity-matrix.md) §B · [quick-start.md](quick-start.md) |
| **SQL parse / bind / route / rewrite** | Supported (whitelist) | XuGu facade + visitors needed for baseline CRUD; binder + route DAL + rewrite modules | Expand parser only as baseline SQL requires; not full XuGu SQL |
| **Sharding (DB / table)** | Supported | Dual-DS (or multi-node) sharding CRUD, joins on sharded order/item patterns | B1 · example YAML [`examples/sharding-two-ds.yaml`](examples/sharding-two-ds.yaml) |
| **Readwrite-splitting (same-host)** | Supported *with topology caveat* | Logical `write_ds` + `read_ds_*`; same-host different DATABASE; optional restricted SELECT-only read user | B2 + T3=A (`docs/topology-same-host.md`): routing to distinct DATABASE names + read-DS privilege deepen. **Never** physical replica lag/isolation |
| **Local TX + savepoint** | Supported | Commit / full rollback / `RELEASE SAVEPOINT` provider | B3 · `XuguSavepointReleaseSQLProvider` |
| **XA wrapper** | Supported (happy-path) | `XuguXAConnectionWrapper` → `com.xugu.xa.XAConnectionImp`; metadata XA DS `com.xugu.xa.XADatasourceImp` | B7 commit/rollback across shards; may Assumption-skip if Atomikos/XuGu XA init fails. Prepare-then-kill evidence = **medium** ([xa-recovery-evidence.md](xa-recovery-evidence.md)); **strong** TM-log recovery **not** claimed. **XA timeout = CLOSED_AS_DEFER** (app/TM-level timeout) |
| **Encrypt** | Supported | Column encrypt/decrypt via SS encrypt rule (no XuGu-specific encrypt SPI) | B6 AES phone column |
| **Pagination** | Supported | Native **`LIMIT`** merge path | [pagination-decision.md](pagination-decision.md) · B5 |
| **Batch DML** | Supported | JDBC batch insert across shards | B4 |
| **Federation stubs** | Supported (stubs) | Federation connection config + safe-empty `FunctionRegister` + `ColumnTypeConverter` | Not a claim of full federated SQL workload coverage |
| **SQLException mapping** | Supported (baseline) | `XuguSQLDialectExceptionMapper` present | Broader error-code map = G-004 P1-4 |

---

## 3. Explicitly NOT supported / DEFER

Do **not** enable, document as supported, or invent no-op SPIs for these items.

| Item | Classification | Reason |
|---|---|---|
| **ShardingSphere Proxy** | NOT supported | Product is JDBC dialect only ([README](../README.md) / quick-start) |
| **MySQL / Oracle / PostgreSQL compatible modes** | NOT supported | Only `compatiblemode=NONE`; no MySQL-trunk fallback or other-mode dialect branch |
| **`DialectDatabasePrivilegeChecker`** | DEFER | XuGu privilege model not mapped to SS checker API; inventing a no-op checker is forbidden ([parity-matrix.md](parity-matrix.md)) |
| **`DialectShardingDALResultMerger` (SHOW DAL)** | DEFER | NONE mode has no MySQL-style SHOW DAL product surface |
| **Full PL/SQL / cold DDL parser** | DEFER | Expand only as baseline SQL requires; full PL/SQL is out of XuGu–SS product scope for now |
| **XA `setTransactionTimeout` / RM XA timeout abort** | **CLOSED_AS_DEFER** | G-006 Q-02: driver stub ignores `setTransactionTimeout` (`COMMITTED_DESPITE_TIMEOUT`); wrapper has no alt API. **Ops workaround:** application/TM-level timeout / cancel **before** 2PC — do not claim RM XA timeout abort ([xa-recovery-evidence.md](xa-recovery-evidence.md)) |
| **Multi-machine / physical read replica topology** | NOT supported (out of Goal) | G-004 explicitly excludes multi-machine / independent physical replicas; same-host simulation only |
| **Remote Maven publish / push / protected Ship** | Human Ship gate | Local `mvn clean install` is the consumer path until authorized |

---

## 4. Baseline scenarios B1–B7 — what “PASS” means

Catalog detail: [baseline-catalog.md](baseline-catalog.md). Run with `-Pbaseline` (or `-Pit-xugu`) against a reachable XuGu host under `compatiblemode=NONE`.

| ID | Scenario | PASS means (IT contract) | Does **not** prove |
|---|---|---|---|
| **B1** | Order DB+table sharding | Place order + item; query by `order_id`; empty miss + duplicate PK fail; 8-thread insert/select smoke | Full SQL surface; cross-region sharding ops |
| **B2** | Readwrite splitting | Insert + select through `write_ds` / `read_ds_*`; empty/dup PK; concurrent write/read; same-host different-DATABASE routing; T3=A restricted read-user INSERT-deny / SELECT-ok when lab allows | Physical replica isolation or lag; multi-machine topology |
| **B3** | Local TX + savepoint | Rollback-to-savepoint keeps earlier rows; full rollback → 0 rows; concurrent local commit smoke | Distributed TX; XA recovery |
| **B4** | Batch insert | ~20-row batch across shards; empty batch no-op; dup-in-batch fails; concurrent batch smoke | Unlimited batch size / pool exhaustion (P1-2) |
| **B5** | Pagination `LIMIT` | `LIMIT 5` ≤5 rows; empty table → 0; concurrent LIMIT smoke | ROWNUM product path (probe-only; strategy is LIMIT) |
| **B6** | Encrypt column | Insert plaintext → select decrypted; empty/dup; concurrent encrypt I/O | Custom algorithms beyond configured AES rule |
| **B7** | XA across shards | XA commit/rollback; dup PK leaves prior counts; concurrent XA commit smoke | TM crash / heuristic recovery (P1-1); may SKIP if XA DS init fails |

**PASS** = required `@Test` methods green on the configured lab host (or documented Assumption skip when host/XA unavailable).  
**PASS ≠** “production hardening complete.”

---

## 5. Production hardening gaps (G-004)

Relative to “一般业务生产可用” under controlled assumptions (G-004 production-hardening design: P0+P1, same-host only):

| Gap | Goal item | Current stance |
|---|---|---|
| Boundary / failure / concurrency per scenario | P0-1 | In progress / catalog lists ≥3 tests per B*; keep evidence current |
| True same-host read DS routing + privilege deepen | P0-2 / G-005 T3=A | Different-DATABASE routing + restricted read user on read DS URLs ([topology-same-host.md](topology-same-host.md)); **never** claim physical replica |
| This support matrix | P0-3 | This document |
| Version 5.5.3-xugu.2 + release notes with known gaps | P0-4 | This release (RELEASE-NOTES-5.5.3-xugu.2.md) |
| XA recovery (kill TM / interrupt / timeout) | P1-1 / G-005 T2 / G-006 Q-02 | Prepare-then-kill **medium** ([xa-recovery-evidence.md](xa-recovery-evidence.md)); timeout **CLOSED_AS_DEFER** (app/TM-level); strong recover **not** proven |
| Load + fault injection report | P1-2 | Not claimed |
| Second namespace / weak second env | P1-3 | Same host only; not multi-site |
| ExceptionMapper expansion + error-code map | P1-4 | Baseline mapper only until expanded |

**Allowed external wording after G-004 Accept:** general business production under whitelist SQL, verified same-host topology, and rollback plan.  
**Forbidden wording:** production-grade stability hardening complete / financial-grade XA / multi-site replica verified.

---

## 6. Quick links

| Doc | Use |
|---|---|
| [parity-matrix.md](parity-matrix.md) | SPI PASS · DEFER (design §3.2 / G-003) |
| [baseline-catalog.md](baseline-catalog.md) | B1–B7 classes, YAML, how to run |
| [quick-start.md](quick-start.md) | ≈30-min consumer install path |
| [pagination-decision.md](pagination-decision.md) | LIMIT vs ROWNUM probe → LIMIT |
| [g003-acceptance.md](g003-acceptance.md) | Prior Goal Accept (baseline + DEFER clearance) |
| [topology-same-host.md](topology-same-host.md) | T3=A same-host deepen limits + BLOCKED_ENV |
| [examples/sharding-two-ds.yaml](examples/sharding-two-ds.yaml) | Dual-DS sharding YAML template |

---

## 7. Summary table

| Category | Items |
|---|---|
| **Supported** | JDBC dialect · sharding · same-host readwrite (topology caveats) · local TX+savepoint · XA wrapper (happy-path) · encrypt · LIMIT pagination · batch · federation stubs · baseline ExceptionMapper |
| **NOT supported** | Proxy · MySQL/Oracle/PG compat modes · multi-machine / physical replica · MySQL trunk fallback |
| **DEFER / CLOSED_AS_DEFER** | PrivilegeChecker · SHOW DAL merger · full PL/SQL parser · XA RM timeout (`setTransactionTimeout`, CLOSED_AS_DEFER) |
| **Hardening open** | P1-1..P1-4 (XA recovery · load/fault · env2 · ExceptionMapper map) |
