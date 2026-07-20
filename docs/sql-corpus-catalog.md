# SQL Business Corpus Catalog (G-005 T1)

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

## Cases

| id | category | SQL / description | expect | status | reason (if DEFER) |
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
| C061 | advanced | `WITH cte AS (SELECT 1 AS N FROM DUAL) SELECT N FROM cte` | both | DEFER | CTE not in XuGu-SS whitelist / parser scope |
| C062 | advanced | `SELECT * FROM CORPUS_T WINDOW w AS (PARTITION BY USER_ID)` | both | DEFER | WINDOW clause out of baseline whitelist |
| C063 | advanced | `MERGE INTO CORPUS_T …` | both | DEFER | MERGE not supported in XuGu-SS dialect whitelist |
| C064 | advanced | hierarchical `CONNECT BY` | both | DEFER | hierarchical CONNECT BY out of scope |
| C065 | advanced | `SELECT * FROM CORPUS_T FOR UPDATE` | both | DEFER | FOR UPDATE locking semantics not corpus-proven on SS path |
| C066 | advanced | `CREATE PROCEDURE …` | both | DEFER | full PL/SQL parser DEFER per support-matrix |
| C067 | advanced | `INTERSECT` set op | both | DEFER | set INTERSECT not in whitelist |
| C068 | advanced | `MINUS` set op | both | DEFER | MINUS set op not in whitelist |
| C069 | advanced | `FULL OUTER JOIN` | both | DEFER | FULL OUTER JOIN / federation sensitive |
| C070 | advanced | `ROWNUM <= 5` pagination | both | DEFER | product pagination strategy is LIMIT not ROWNUM |
| C071 | advanced | ANSI `OFFSET … FETCH` | both | DEFER | ANSI OFFSET/FETCH disabled for XuGu rewrite |
| C072 | advanced | `CREATE INDEX …` | both | DEFER | cold DDL index not required for corpus PASS gate |
| C073 | advanced | `ALTER TABLE … ADD …` | both | DEFER | ALTER TABLE cold DDL expand-only-if-needed |
| C074 | advanced | `TRUNCATE TABLE CORPUS_T` | both | DEFER | TRUNCATE through SS not corpus-proven |
| C075 | advanced | `INSERT … SELECT …` | both | DEFER | INSERT..SELECT federation/bind edge |
| C076 | xa | XA prepare-only corpus probe | execute | DEFER | XA prepare-kill is G-005 T2 not T1 corpus |
| C077 | privilege | `GRANT SELECT …` | both | DEFER | PrivilegeChecker / GRANT path DEFER |
| C078 | dal | `SHOW TABLES` | both | DEFER | SHOW DAL merger DEFER per support-matrix |
| C079 | advanced | `JSON_OBJECT(…)` | both | DEFER | JSON functions not in whitelist |
| C080 | advanced | `REGEXP_LIKE(…)` | both | DEFER | regex helper not corpus-scoped |

## Summary counts (lab 2026-07-20)

| Triaged | PASS | DEFER | FAIL |
|---|---|---|---|
| 80 | 60 | 20 | 0 |

Observed via `-Psql-corpus`: `SQL_CORPUS triaged=80 PASS=60 DEFER=20 executed=60` · Tests run: 1, Failures: 0.

## How to run

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" -pl tests-it -am test "-Psql-corpus"
```

Regression:

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" -pl tests-it -am test "-Pbaseline"
```
