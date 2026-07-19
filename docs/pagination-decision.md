# XuGu Pagination Decision (compatiblemode=NONE)

**Date:** 2026-07-20  
**Probe:** `PaginationProbeIT` via `com.xugu.cloudjdbc.Driver`  
**URL:** `jdbc:xugu://192.168.2.239:5138/SYSTEM?compatiblemode=NONE&charset=UTF8`

## Probe results

| Check | Result |
|---|---|
| `LIMIT_OK` | `true` |
| `ROWNUM_OK` | `true` |

Commands exercised:

```sql
SELECT 1 AS ID FROM DUAL LIMIT 1
SELECT * FROM (SELECT 1 AS ID FROM DUAL) T WHERE ROWNUM <= 1
```

## Chosen strategy

**`pagination.strategy=LIMIT`**

Both LIMIT and ROWNUM succeed under `compatiblemode=NONE`. Prefer LIMIT because:

1. Probe proves native LIMIT is reliable on the target host/driver (`LIMIT_OK=true`).
2. LIMIT maps to ShardingSphere’s default LIMIT/OFFSET rewrite path and avoids Oracle-style nested `ROWNUM` wrappers.
3. NONE mode is the product contract; choosing the simpler native syntax reduces dialect-specific rewrite surface for M1+.

## DialectPaginationOption

Final configuration in `XuguDatabaseMetaData#getPaginationOption()`:

```java
new DialectPaginationOption(false, "", false)
```

Meaning:

- `containsRowNumber=false` — do **not** treat pagination as ROWNUM/ROW_NUMBER rewrite
- `rowNumberColumnName=""` — unused when row-number pagination is disabled
- `containsTop=false` — no TOP clause

## Rewrite constraints

- Pagination rewrite **must** emit `LIMIT` / `LIMIT … OFFSET …` (or equivalent LIMIT-based forms supported by XuGu NONE).
- **Disable** `OFFSET/FETCH` (ANSI) rewrite for XuGu.
- **Do not** rewrite to `WHERE ROWNUM <= n` / nested ROWNUM windows for the NONE-mode dialect path, even though ROWNUM also works on the live host.
