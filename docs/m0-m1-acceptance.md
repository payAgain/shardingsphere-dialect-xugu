# M0–M1 Acceptance Gate (G-001)

**Date:** 2026-07-20  
**Repo:** `shardingsphere-dialect-xugu`  
**Goal:** G-001 XuGu native JDBC dialect (M0–M1)  
**Unit-test gate SHA (this acceptance):** see commit that adds this file  
**NativeCrudIT PASS SHA:** `71985195fdc2645b93a34c79595c5604fb6c2f67`

## Verification commands

```powershell
# Unit only (Surefire excludes **/*IT.java and **/it/**)
mvn -q test

# Live IT profile
mvn -pl tests-it -am test -Pit-xugu -Dtest=NativeCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

**Unit `mvn -q test`:** BUILD SUCCESS (2026-07-20)  
**Module totals (Failures/Errors/Skipped = 0):** connector 18 + parser-engine 11 + binder 6 + jdbc-dialect 1 = **36**; `tests-it` excluded by default  
**NativeCrudIT (`-Pit-xugu`):** PASS — Tests run: 1, Failures: 0 (reconfirmed at acceptance; originally recorded at `71985195`)

## Checklist evidence

| Item | Evidence | Result |
|---|---|---|
| No trunk | `XuguDatabaseTypeTest#assertNoTrunkDatabaseType` — `assertFalse(databaseType.getTrunkDatabaseType().isPresent())`; SHA `65fc259` introduced native `DatabaseType` without trunk | PASS |
| Five connector SPI | `META-INF/services`: `DatabaseType`, `ConnectionPropertiesParser`, `DialectDatabaseMetaData`, `DialectResultSetMapper`, `DialectMetaDataLoader`; covered by connector unit tests (`XuguDatabaseTypeTest`, `XuguConnectionPropertiesParserTest`, `XuguDatabaseMetaDataTest`, `XuguResultSetMapperTest`, `XuguMetaDataLoaderTest`) | PASS |
| Pagination decision | `docs/pagination-decision.md` — live probe `LIMIT_OK=true` / `ROWNUM_OK=true` → strategy **LIMIT**; `DialectPaginationOption(false, "", false)` | PASS |
| Syntax whitelist + parser tests | `docs/syntax-whitelist-m1.md` + `XuguParserTest` (11 cases) BUILD SUCCESS | PASS |
| Native CRUD IT | `NativeCrudIT` PASS at SHA `71985195fdc2645b93a34c79595c5604fb6c2f67` (CREATE/INSERT/SELECT/UPDATE/DELETE/DROP, `compatiblemode=NONE`); reconfirmed PASS under `-Pit-xugu` at acceptance | PASS |
| No Oracle XA | `XuguDatabaseMetaDataTest#assertGetTransactionOption` — `xaDriverClassNames` empty and no name containing `OracleXA` | PASS |

## Surefire separation

Parent `pom.xml` default Surefire excludes:

- `**/*IT.java`
- `**/it/**`

Profile `it-xugu` clears those excludes and includes IT patterns so live tests run only when opted in.

## Default schema note

`XuguSchemaOption` default schema is lowercase `sysdba`; `XuguDatabaseMetaDataTest#assertGetSchemaOption` expects `Optional.of("sysdba")` — unit suite green (no expectation fix required at acceptance).

## Recent commits (`git log --oneline -15`)

```text
7198519 test(it): verify XuGu native dialect CRUD on NONE mode
353ad82 feat(binder): add XuGu projection identifier extractor for M1
4ab6ca0 feat(parser): add XuGu native ANTLR facade for M1 SQL whitelist
1b49fd7 feat: add jdbc-dialect-xugu aggregator packaging connector
494ad80 feat(connector): add XuGu ALL_* metadata loader with unit tests
103a2ad test(it): decide XuGu NONE pagination strategy from live probe
c386ba6 feat(connector): add XuGu dialect metadata options and result set mapper
65fc259 feat(connector): add XuGu DatabaseType and JDBC URL parser without trunk
69daa42 chore: bootstrap dialect-xugu multi-module parent for SS 5.5.3
```

## M1 exit

All checklist rows have evidence → M0–M1 gate **PASS**. M2 (shard Route/Rewrite) may proceed.
