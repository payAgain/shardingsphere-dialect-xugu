# XuGu M1 Syntax Whitelist

**Database type:** `XuGu`  
**Pagination:** `LIMIT` (see `docs/pagination-decision.md`)  
**Parser modules:** `parser-sql-statement-xugu`, `parser-sql-engine-xugu`

## Whitelist SQLs

```sql
SELECT 1 FROM DUAL
SELECT id, name FROM t_order WHERE id = 1
INSERT INTO t_order (id, name) VALUES (1, 'a')
UPDATE t_order SET name = 'b' WHERE id = 1
DELETE FROM t_order WHERE id = 1
CREATE TABLE t_order (id INT PRIMARY KEY, name VARCHAR(64))
DROP TABLE t_order
COMMIT
ROLLBACK
SELECT 1 FROM DUAL LIMIT 1
```

## Expected statement kinds

| SQL | Kind |
|---|---|
| `SELECT ...` / `SELECT ... LIMIT` | Select |
| `INSERT ...` | Insert |
| `UPDATE ...` | Update |
| `DELETE ...` | Delete |
| `CREATE TABLE ...` | CreateTable |
| `DROP TABLE ...` | DropTable |
| `COMMIT` | Commit |
| `ROLLBACK` | Rollback |

## Verification

```powershell
mvn -pl parser-sql-engine-xugu,jdbc-dialect-xugu -am test
```

Unit coverage: `XuguParserTest` parses each whitelist SQL via `SQLParserEngine` + `SQLStatementVisitorEngine` with databaseType `XuGu`.

### Live IT (compatiblemode=NONE)

```powershell
mvn -pl tests-it -am test -Dtest=NativeCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

Result (2026-07-20): **PASS** — `NativeCrudIT` ran CREATE / INSERT / SELECT / UPDATE / DELETE / DROP end-to-end through ShardingSphere-JDBC single datasource against `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8`. Host-unreachable runs skip via JUnit `Assumptions`.
