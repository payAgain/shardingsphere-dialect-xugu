# Baseline IT Catalog (B1–B7)

> Repo: `shardingsphere-dialect-xugu` · Dialect: XuGu `compatiblemode=NONE` · Host defaults: `tests-it/src/test/resources/it-xugu.properties`

## How to run

Unit suite (default, excludes `*IT`):

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q test
```

Baseline IT only:

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test "-Pbaseline" "-Dsurefire.failIfNoSpecifiedTests=false"
```

All IT including baseline (profile `it-xugu`):

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test "-Pit-xugu" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Unreachable XuGu host → JUnit Assumption **SKIP** (not failure).

Shared fixture: `com.xugudb.shardingsphere.it.baseline.BaselineSupport`.

Each B1–B7 class has ≥3 `@Test` methods: happy-path, boundary/failure, concurrency smoke (8 threads + `CountDownLatch`).

### B2 readwrite limits (P0-2)

- Topology: **same XuGu host**, `write_ds` → `baseline_write`, `read_ds_*` → `baseline_read0` / `baseline_read1` (different DATABASE, not a physical replica).
- Routing asserts via JDBC URL path + row presence on write vs read DATABASE (sql-show remains enabled in YAML for manual log inspection).
- Do **not** claim replica lag, streaming replication, or physical read-only isolation from these ITs.

---

## Catalog

| ID | Class | YAML | `@Test` methods | Asserts |
|---|---|---|---|---|
| B1 | `OrderDbTableShardingIT` | `baseline/baseline-order-sharding.yaml` | `placeOrderQueryAndJoin`; `queryMissingOrderReturnsEmptyAndDuplicateKeyFails`; `concurrentPlaceOrdersSmoke` | DB+table sharding on `baseline_order` / `baseline_order_item`; place order + item; query by `order_id`; empty miss + duplicate PK; 8-thread insert/select |
| B2 | `ReadwriteSplittingIT` | `baseline/baseline-readwrite.yaml` | `writeThenReadSmoke`; `selectMissingIdReturnsEmptyAndDuplicateKeyFails`; `concurrentWriteReadSmoke`; `sameHostReadDsRoutingIsolation` | Same-host different DATABASE (`baseline_write` / `baseline_read0` / `baseline_read1`): INSERT→write only; auto-commit SELECT→read (marker / miss write row); in-TX SELECT (PRIMARY)→write. **Not** physical replica lag/isolation |
| B3 | `LocalTransactionSavepointIT` | `baseline/baseline-sharding-db.yaml` | `rollbackToSavepointKeepsEarlierRows`; `fullRollbackLeavesNoRows`; `concurrentLocalTxSmoke` | savepoint keep; full rollback → 0 rows; 8-thread local commit |
| B4 | `BatchInsertIT` | `baseline/baseline-sharding-db.yaml` | `batchInsertAcrossShards`; `emptyBatchIsNoOpAndDuplicateKeyInBatchFails`; `concurrentBatchInsertSmoke` | batch ~20 rows 10/10; empty batch + duplicate batch fails; 8×2 concurrent batch |
| B5 | `PaginationListIT` | `baseline/baseline-sharding-db.yaml` | `limitReturnsAtMostFiveRows`; `limitOnEmptyTableReturnsZero`; `concurrentPaginationSmoke` | `LIMIT 5` ≤5; empty LIMIT → 0; 8-thread concurrent LIMIT 3 |
| B6 | `EncryptColumnIT` | `baseline/baseline-encrypt.yaml` | `insertPlaintextSelectDecrypted`; `selectMissingUserReturnsEmptyAndDuplicateKeyFails`; `concurrentEncryptInsertSelectSmoke` | AES phone encrypt/decrypt; empty miss + duplicate PK; 8-thread encrypt I/O |
| B7 | `XATransactionIT` | `baseline/baseline-xa.yaml` | `xaCommitAndRollbackAcrossShards`; `xaDuplicateKeyFailsAndLeavesPriorCounts`; `concurrentXaCommitSmoke` | XA commit/rollback; duplicate PK leaves prior counts; 8-thread XA commit |

## Physical naming

XuGu stores unquoted identifiers as **UPPER_CASE**. Logic tables use lowercase; physical tables use `BASELINE_*` prefixes to avoid clash with other ITs.

## B7 SKIP notes

If Atomikos / XuGu XA init fails, `XATransactionIT` Assumptions-skips with message prefix `XA DataSource init failed` or `XA commit path failed`. Dependencies: `shardingsphere-transaction-xa-atomikos` (+ Atomikos transitive). XA driver class from dialect metadata: `com.xugu.xa.XADatasourceImp`.
