# Release Notes — `5.5.3-xugu.2`

**Date:** 2026-07-20  
**Artifact:** `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu:5.5.3-xugu.2`  
**Upstream:** Apache ShardingSphere JDBC `5.5.3`  
**Driver:** `com.xugudb:xugu-jdbc:12.3.6`  
**Compatible mode:** **`compatiblemode=NONE` only**

This is a **local-install** release prep bump (consumer path: `mvn clean install`). Tag / push / GitHub Release remain Human Ship gates.

---

## What's new

### G-003 baseline (carried into this release)

- XuGu-native SPI clearance for Savepoint, XA wrapper, SQLDialectExceptionMapper, and federation stubs (FunctionRegister / ColumnTypeConverter).
- Baseline IT suite **B1–B7** (sharding, readwrite-splitting, local TX + savepoint, batch, LIMIT pagination, encrypt, XA) under `compatiblemode=NONE`.
- Catalog: [baseline-catalog.md](baseline-catalog.md) · acceptance: [g003-acceptance.md](g003-acceptance.md).

### G-004 P0 expansions

| Item | Change |
|---|---|
| **P0-1** | B1–B7 expanded with boundary / empty / duplicate-key and concurrency smoke cases |
| **P0-2** | Same-host **read DS routing isolation** asserts (different DATABASE on one host — not a physical replica) |
| **P0-3** | Production [support-matrix.md](support-matrix.md) published |
| **P0-4** | Version bump **`5.5.3-xugu.1` → `5.5.3-xugu.2`** + these notes |

### Support matrix

External capability whitelist for controlled production evaluation: [docs/support-matrix.md](support-matrix.md).

### Same-host readwrite routing asserts

Readwrite-splitting (B2) now asserts that read traffic can be isolated to a **same-host, different DATABASE** read DS configuration. This validates logical write/read DS routing in the lab topology; it does **not** prove multi-machine or physical replica lag/isolation.

---

## Coordinates

| Artifact | Version |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | `5.5.3-xugu.2` |
| Upstream `org.apache.shardingsphere:shardingsphere-jdbc` | `5.5.3` |
| XuGu driver `com.xugudb:xugu-jdbc` | `12.3.6` |

```xml
<dependency>
  <groupId>com.xugudb.shardingsphere</groupId>
  <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
  <version>5.5.3-xugu.2</version>
</dependency>
```

---

## Known limits

| Limit | Status |
|---|---|
| **No multi-machine / physical replica** | Out of Goal. Same-host (or same-host different DATABASE) simulation only |
| **XA recovery** | Happy-path XA wrapper only; crash / kill-TM / heuristic recovery remains **shallow** until G-004 **P1** evidence docs land |
| **`DialectDatabasePrivilegeChecker`** | **DEFER** (XuGu privilege model not mapped; no no-op checker) |
| **SHOW DAL / `DialectShardingDALResultMerger`** | **DEFER** (no MySQL-style SHOW surface under NONE) |
| **Full PL/SQL / cold DDL parser** | **DEFER** (expand only as baseline SQL requires) |
| **`compatiblemode=NONE` only** | Other XuGu compatible modes and MySQL-trunk fallback are out of scope |
| **ShardingSphere Proxy** | **Supported (whitelist)** — MySQL wire frontend, XuGu `compatiblemode=NONE` storage; not OSS trunk path ([proxy-quick-start.md](proxy-quick-start.md); live IT pending lab recovery) |

See also [support-matrix.md](support-matrix.md) §3 / §5 and [parity-matrix.md](parity-matrix.md).

---

## Upgrade from `5.5.3-xugu.1`

1. Change dependency version to `5.5.3-xugu.2`.
2. Keep JDBC URLs / props with **`compatiblemode=NONE`**.
3. Re-run baseline or smoke IT against your lab host if you rely on B1–B7 evidence.
4. Do not assume XA recovery or multi-site HA from this bump alone.

---

## Build / verify (local)

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install "-DskipTests"
# or with unit tests (IT excluded by Surefire default):
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install
```

Quick start: [quick-start.md](quick-start.md).
