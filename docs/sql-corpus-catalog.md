# SQL Business Corpus Catalog (G-005 T1 / G-006 Q-03)

**Dialect:** XuGu · `compatiblemode=NONE` only  
**Lab:** `jdbc:xugu://192.168.2.239:5138/…` (isolated DATABASE prefix `corpus_*`)  
**Runner:** `tests-it/.../corpus/SqlCorpusIT.java` · Maven profile `-Psql-corpus`  
**Source of truth (machine):** `tests-it/src/test/resources/corpus/corpus-cases.tsv`  
**Version:** `5.5.3-xugu.2` (frozen)

## Gates

| Metric | Target | Notes |
|---|---|---|
| Triaged | ≥60 | PASS + DEFER |
| PASS | ≥40 | executed/parsed on lab |
| FAIL | 0 | DEFER allowed with reason |
| Q-03 DEFER reduction | DEFER ≤10 **or** newly promoted PASS ≥8 | Lab triage 2026-07-20 |

## Expect

| Value | Meaning |
|---|---|
| `parse` | XuGu `SQLParserEngine` + visitor only |
| `execute` | native JDBC and/or ShardingSphere JDBC |
| `both` | parse then execute |

## Channel

| Value | Meaning |
|---|---|
| `native` | raw XuGu JDBC against `corpus_ds0` |
| `ss` | ShardingSphere JDBC single DS (`corpus-single-ds.yaml`) |
| `ss_shard` | ShardingSphere dual-DS shard (`corpus-shard.yaml`) |
| `parse` | parser only |

## Q-03 triage (C061–C080)

| Class | Meaning | IDs |
|---|---|---|
| **A** | Promote to PASS with existing parser + native/SS execute (minimal SQL fix only) | C061, C063, C065, C066, C067, C068, C069, C070, C072, C073, C074, C075, C077, C079, C080 |
| **B** | Parse-only PASS (execute unsafe or rejected by XuGu) | C064, C071 |
| **C** | Permanent product DEFER | C062, C076, C078 |

## Cases

| id | category | SQL / description | expect | status | reason (if DEFER) / triage |
|---|---|---|---|---|---|
| C001 | select | `SELECT 1 FROM DUAL` | both | PASS | |
| C002 | select | `SELECT 1 AS N FROM DUAL` | both | PASS | |
| C003 | select | `SELECT 1 + 1 AS N FROM DUAL` | both | PASS | |
| C004 | select | `SELECT 'xugu' AS S FROM DUAL` | both | PASS | |
| C005 | select | `SELECT NULL AS N FROM DUAL` | both | PASS | |
| C006 | parse | `SELECT 1 FROM DUAL` | parse | PASS | |
| C007 | parse | `SELECT id, name FROM t_order WHERE id = 1` | parse | PASS | |
| C008 | parse | `INSERT INTO t_order (id, name) VALUES (1, 'a')` | parse | PASS | |
| C009 | parse | `UPDATE t_order SET name = 'b' WHERE id = 1` | parse | PASS | |
| C010 | parse | `DELETE FROM t_order WHERE id = 1` | parse | PASS | |
| C011 | parse | `CREATE TABLE t_order (id INT PRIMARY KEY, name VARCHAR(64))` | parse | PASS | |
| C012 | parse | `DROP TABLE t_order` | parse | PASS | |
| C013 | parse | `COMMIT` | parse | PASS | |
| C014 | parse | `ROLLBACK` | parse | PASS | |
| C015 | parse | `SELECT 1 FROM DUAL LIMIT 1` | parse | PASS | |
| C016 | ddl | `CREATE TABLE CORPUS_SIMPLE (ID INT PRIMARY KEY, NAME VARCHAR(64))` | execute | PASS | |
| C017 | ddl | `DROP TABLE CORPUS_SIMPLE` | execute | PASS | |
| C018 | dml | `INSERT INTO CORPUS_T (...) VALUES (1, 1, 'NEW', 10, 'a')` | execute | PASS | |
| C019 | dml | `SELECT ID, NAME FROM CORPUS_T WHERE ID = 1` | execute | PASS | |
| C020 | dml | `UPDATE CORPUS_T SET NAME = 'b' WHERE ID = 1` | execute | PASS | |
| C021 | dml | `DELETE FROM CORPUS_T WHERE ID = 1` | execute | PASS | |
| C022 | dml | `INSERT INTO CORPUS_T (...) VALUES (2, 2, 'NEW', 20, 'c')` via SS | execute | PASS | |
| C023 | dml | `SELECT ID, STATUS FROM CORPUS_T WHERE ID = 2` via SS | execute | PASS | |
| C024 | dml | `UPDATE CORPUS_T SET STATUS = 'PAID' WHERE ID = 2` via SS | execute | PASS | |
| C025 | dml | `DELETE FROM CORPUS_T WHERE ID = 2` via SS | execute | PASS | |
| C026 | select | `SELECT COUNT(*) FROM CORPUS_T` | execute | PASS | |
| C027 | select | `SELECT ID FROM CORPUS_T WHERE USER_ID = 1` | execute | PASS | |
| C028 | select | `SELECT ID FROM CORPUS_T WHERE STATUS = 'NEW'` | execute | PASS | |
| C029 | select | `SELECT ID FROM CORPUS_T WHERE AMT >= 10` | execute | PASS | |
| C030 | select | `SELECT ID FROM CORPUS_T WHERE NAME LIKE 'a%'` | execute | PASS | |
| C031 | select | `SELECT ID FROM CORPUS_T WHERE ID IN (1, 2, 3)` | execute | PASS | |
| C032 | select | `SELECT ID FROM CORPUS_T WHERE AMT BETWEEN 1 AND 100` | execute | PASS | |
| C033 | select | `SELECT ID FROM CORPUS_T WHERE NAME IS NOT NULL` | execute | PASS | |
| C034 | limit | `SELECT ID FROM CORPUS_T LIMIT 5` | execute | PASS | |
| C035 | limit | `SELECT ID FROM CORPUS_T LIMIT 3` via SS | execute | PASS | |
| C036 | limit | `SELECT ID FROM CORPUS_T ORDER BY ID LIMIT 2` | execute | PASS | |
| C037 | limit | `SELECT ID FROM CORPUS_T LIMIT 1` via SS | execute | PASS | |
| C038 | order | `SELECT ID FROM CORPUS_T ORDER BY ID ASC` | execute | PASS | |
| C039 | order | `SELECT ID FROM CORPUS_T ORDER BY ID DESC` | execute | PASS | |
| C040 | agg | `SELECT SUM(AMT) FROM CORPUS_T` | execute | PASS | |
| C041 | agg | `SELECT MAX(AMT) FROM CORPUS_T` | execute | PASS | |
| C042 | agg | `SELECT MIN(AMT) FROM CORPUS_T` | execute | PASS | |
| C043 | agg | `SELECT AVG(AMT) FROM CORPUS_T` | execute | PASS | |
| C044 | tx | scenario: commit keeps row (native) | execute | PASS | |
| C045 | tx | scenario: rollback drops row (native) | execute | PASS | |
| C046 | tx | scenario: savepoint rollback keeps earlier (SS) | execute | PASS | |
| C047 | shard | `INSERT INTO corpus_order (id, user_id, status) VALUES (10, 1, 'NEW')` | execute | PASS | |
| C048 | shard | `SELECT id, status FROM corpus_order WHERE id = 10 AND user_id = 1` | execute | PASS | |
| C049 | shard | `INSERT INTO corpus_order (id, user_id, status) VALUES (11, 2, 'NEW')` | execute | PASS | |
| C050 | shard | `SELECT id FROM corpus_order WHERE user_id = 2 AND id = 11` | execute | PASS | |
| C051 | shard | `UPDATE corpus_order SET status = 'OK' WHERE id = 10 AND user_id = 1` | execute | PASS | |
| C052 | shard | `DELETE FROM corpus_order WHERE id = 11 AND user_id = 2` | execute | PASS | |
| C053 | batch | scenario: batch insert 3 rows (native) | execute | PASS | |
| C054 | dml | seed insert id=100 `STATUS='SEED'` | execute | PASS | |
| C055 | dml | seed insert id=101 `STATUS='SEED'` | execute | PASS | |
| C056 | dml | seed insert id=102 `STATUS='SEED'` | execute | PASS | |
| C057 | select | `SELECT ID FROM CORPUS_T WHERE USER_ID = 1 AND STATUS = 'SEED'` via SS | execute | PASS | |
| C058 | limit | `SELECT ID FROM CORPUS_T WHERE STATUS = 'SEED' LIMIT 2` via SS | execute | PASS | |
| C059 | select | `SELECT DISTINCT STATUS FROM CORPUS_T` | execute | PASS | |
| C060 | select | `SELECT ID FROM CORPUS_T WHERE ID <> 0` | execute | PASS | |
| C061 | advanced | `WITH cte AS (SELECT 1 AS N FROM DUAL) SELECT N FROM cte` | both | PASS | A: CTE parse+native+SS |
| C062 | advanced | `SELECT * FROM CORPUS_T WINDOW w AS (PARTITION BY USER_ID)` | both | DEFER | C: WINDOW rejected by XuGu + SS parser |
| C063 | advanced | `MERGE INTO CORPUS_T …` | both | PASS | A: MERGE parse+native |
| C064 | advanced | hierarchical `CONNECT BY` | parse | PASS | B: parse OK; execute infinite-loop on lab |
| C065 | advanced | `SELECT * FROM CORPUS_T FOR UPDATE` | both | PASS | A: FOR UPDATE via SS |
| C066 | advanced | `CREATE PROCEDURE …` | both | PASS | A: procedure DDL native (cleanup DROP PROCEDURE) |
| C067 | advanced | `INTERSECT` set op | both | PASS | A |
| C068 | advanced | `MINUS` set op | both | PASS | A |
| C069 | advanced | `FULL OUTER JOIN` | both | PASS | A: single-DS native (not shard federation) |
| C070 | advanced | `ROWNUM <= 5` | both | PASS | A: lab OK; product pagination strategy remains LIMIT |
| C071 | advanced | ANSI `OFFSET … FETCH` | parse | PASS | B: parse OK; XuGu rejects execute |
| C072 | advanced | `CREATE INDEX …` | both | PASS | A: cold index DDL native |
| C073 | advanced | `ALTER TABLE … ADD …` | both | PASS | A: cold ALTER native |
| C074 | advanced | `TRUNCATE TABLE CORPUS_T` | both | PASS | A: TRUNCATE via SS |
| C075 | advanced | `INSERT … SELECT …` (explicit columns) | both | PASS | A: SS path; column list avoids ALTER widen mismatch |
| C076 | xa | XA prepare-only corpus probe | execute | DEFER | C: XA recovery is Q-01 track |
| C077 | privilege | `GRANT SELECT …` | both | PASS | A: GRANT SQL OK; PrivilegeChecker SPI still DEFER |
| C078 | dal | `SHOW TABLES` | both | DEFER | C: SHOW DAL not in NONE-mode product surface |
| C079 | advanced | `JSON_OBJECT(…)` | execute | PASS | A: native execute; XuGu-SS JSON parse still incomplete |
| C080 | advanced | `REGEXP_LIKE(…)` | both | PASS | A |

## Summary counts (lab 2026-07-20, Q-03)

| Triaged | PASS | DEFER | FAIL | Newly promoted |
|---|---|---|---|---|
| 80 | 77 | 3 | 0 | 17 |

Before Q-03: `PASS=60 DEFER=20`. After: `PASS=77 DEFER=3` (gate: DEFER≤10 and promoted≥8).

Observed via `-Psql-corpus`: see latest IT stdout `SQL_CORPUS triaged=80 PASS=77 DEFER=3`.

## How to run

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" -pl tests-it -am test "-Psql-corpus"
```

Regression:

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" -pl tests-it -am test "-Pbaseline"
```
