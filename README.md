# shardingsphere-dialect-xugu

Apache ShardingSphere **XuGu native JDBC dialect** plugin for **5.5.3**, released as **`5.5.3-xugu`**.

**Start here:** [docs/quick-start.md](docs/quick-start.md) (≈30-minute JDBC path) · **Proxy:** [docs/proxy-quick-start.md](docs/proxy-quick-start.md) (MySQL wire → XuGu NONE storage).
**Support matrix:** [docs/support-matrix.md](docs/support-matrix.md) · **Release notes:** [docs/RELEASE-NOTES-5.5.3-xugu.md](docs/RELEASE-NOTES-5.5.3-xugu.md).

## Release coordinates

| Artifact | Version |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | `5.5.3-xugu` |
| Upstream `org.apache.shardingsphere:shardingsphere-jdbc` | `5.5.3` |
| XuGu driver `com.xugudb:xugu-jdbc` | `12.3.6` (install into local `.m2` first) |

```xml
<dependency>
  <groupId>com.xugudb.shardingsphere</groupId>
  <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
  <version>5.5.3-xugu</version>
</dependency>
```

## Scope (read this)

| In scope | Out of scope |
|---|---|
| JDBC dialect SPI (`getDatabaseType() == "XuGu"`) | Other XuGu compatible modes |
| **ShardingSphere Proxy** — **MySQL wire** frontend, **XuGu `compatiblemode=NONE`** storage/dialect ([proxy-quick-start.md](docs/proxy-quick-start.md); not OSS trunk MySQL parser/backend) | **MySQL trunk** fallback / OSS trunk Proxy path |
| **`compatiblemode=NONE` only** | Remote publish (no push required for local use) |
| Sharding CRUD + LIMIT pagination | Unverified production SLA (live Proxy IT pending lab recovery) |
| Local Maven install / consumer apps | |

Example YAML: [docs/examples/sharding-two-ds.yaml](docs/examples/sharding-two-ds.yaml).

## Build

```powershell
# Install XuGu JDBC once
mvn install:install-file -Dfile=path\to\xugu-jdbc-12.3.6.jar -DgroupId=com.xugudb -DartifactId=xugu-jdbc -Dversion=12.3.6 -Dpackaging=jar

# Unit tests (Surefire excludes *IT)
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q test

# Install release into local .m2
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install
```

## Integration tests

Default host: `192.168.2.239:5138` · credentials in `tests-it/src/test/resources/it-xugu.properties` · **`compatiblemode=NONE`**. Unreachable hosts → Assumption skip.

```powershell
# Dual-DS sharding CRUD + LIMIT
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test -Pit-xugu -Dtest=ShardingCrudIT -Dsurefire.failIfNoSpecifiedTests=false

# Single-DS native CRUD
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -pl tests-it -am test -Pit-xugu -Dtest=NativeCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

## Documentation

| Doc | Purpose |
|---|---|
| [docs/quick-start.md](docs/quick-start.md) | 30-min JDBC consumer path |
| [docs/proxy-quick-start.md](docs/proxy-quick-start.md) | Proxy: MySQL wire frontend → XuGu NONE storage |
| [docs/support-matrix.md](docs/support-matrix.md) | Production support matrix (whitelist + known limits) |
| [docs/RELEASE-NOTES-5.5.3-xugu.md](docs/RELEASE-NOTES-5.5.3-xugu.md) | 5.5.3-xugu release notes |
| [docs/examples/sharding-two-ds.yaml](docs/examples/sharding-two-ds.yaml) | Dual-DS sharding YAML (placeholders) |
| [docs/m0-m1-acceptance.md](docs/m0-m1-acceptance.md) | M0–M1 acceptance (connector + parser + NativeCrudIT) |
| [docs/m2-acceptance.md](docs/m2-acceptance.md) | M2 acceptance (route/rewrite + ShardingCrudIT) |
| [docs/m3-acceptance.md](docs/m3-acceptance.md) | M3 acceptance (system DB / bind / federation + parity) |
| [docs/m4-acceptance.md](docs/m4-acceptance.md) | M4 acceptance (release + smoke; added at M4-3) |
| [docs/parity-matrix.md](docs/parity-matrix.md) | SPI / capability PASS·DEFER matrix |
| [docs/pagination-decision.md](docs/pagination-decision.md) | LIMIT vs ROWNUM probe → LIMIT |
| [docs/syntax-whitelist-m1.md](docs/syntax-whitelist-m1.md) | Parser syntax whitelist |
