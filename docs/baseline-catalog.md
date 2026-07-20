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

---

## Catalog

| ID | Class | YAML | Asserts |
|---|---|---|---|
| B1 | `OrderDbTableShardingIT` | `baseline/baseline-order-sharding.yaml` | DB+table sharding on `baseline_order` / `baseline_order_item`; place order + item; query by `order_id`; two-query association |
| B2 | `ReadwriteSplittingIT` | `baseline/baseline-readwrite.yaml` | `write_ds` + `read_ds_0/1`; insert + select; `sql-show: true`. IT maps read URLs to write physical DB (no replica) |
| B3 | `LocalTransactionSavepointIT` | `baseline/baseline-sharding-db.yaml` | `autoCommit=false`; insert; savepoint; insert; rollback to savepoint; commit; row count |
| B4 | `BatchInsertIT` | `baseline/baseline-sharding-db.yaml` | `PreparedStatement.addBatch` ~20 rows across shard keys; physical counts 10/10 |
| B5 | `PaginationListIT` | `baseline/baseline-sharding-db.yaml` | Seed rows; `SELECT ... ORDER BY id LIMIT 5`; result size ≤ 5 (requires LIMIT visitor → pagination merge) |
| B6 | `EncryptColumnIT` | `baseline/baseline-encrypt.yaml` | AES encrypt on `phone`; SS select returns plaintext; physical `PHONE_CIPHER` ≠ plaintext |
| B7 | `XATransactionIT` | `baseline/baseline-xa.yaml` | `transaction.defaultType=XA` + Atomikos; commit across ds0/ds1; rollback leaves prior counts |

## Physical naming

XuGu stores unquoted identifiers as **UPPER_CASE**. Logic tables use lowercase; physical tables use `BASELINE_*` prefixes to avoid clash with other ITs.

## B7 SKIP notes

If Atomikos / XuGu XA init fails, `XATransactionIT` Assumptions-skips with message prefix `XA DataSource init failed` or `XA commit path failed`. Dependencies: `shardingsphere-transaction-xa-atomikos` (+ Atomikos transitive). XA driver class from dialect metadata: `com.xugu.xa.XADatasourceImp`.
