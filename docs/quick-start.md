# Quick Start (≈30 minutes) — ShardingSphere XuGu dialect `5.5.3-xugu`

Goal: install the dialect into the local Maven repo, wire a dual-datasource sharding app (or run the project IT), and verify CRUD under **`compatiblemode=NONE` only**.

## Prerequisites

- JDK 8+ (ANTLR parser modules need a toolchain that can compile the grammar; project targets Java 8 bytecode)
- Maven 3.9+
- XuGu JDBC driver JAR `xugu-jdbc` **12.3.6**
- Reachable XuGu instance (IT defaults below)

## 1. Install XuGu JDBC into local `.m2`

**Always pass explicit GAV.** Some driver JARs embed Maven metadata that still says `12.3.4`; do **not** rely on auto-detection from the JAR.

```powershell
mvn org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file `
  "-Dfile=path\to\xugu-jdbc-12.3.6.jar" `
  "-DgroupId=com.xugudb" `
  "-DartifactId=xugu-jdbc" `
  "-Dversion=12.3.6" `
  "-Dpackaging=jar" `
  "-DgeneratePom=true"
```

## 2. Build & install this dialect (release coordinates)

### Option A — from source (recommended for contributors)

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install "-DskipITs"
```

### Option B — from GitHub Release ZIP

```powershell
# After downloading the release zip + xugu-jdbc-12.3.6.jar:
.\scripts\install-release-assets.ps1 `
  -ZipPath path\to\shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip `
  -JdbcJar path\to\xugu-jdbc-12.3.6.jar
```

The ZIP **must** contain `shardingsphere-dialect-xugu-parent-5.5.3-xugu.pom`. Install order is parent → JDBC → module JAR+POM → proxy aggregate POM. See `dist/RELEASE-BODY.md`.

Published locally (among others):

| Coordinate | Version |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | **`5.5.3-xugu`** |
| Upstream `org.apache.shardingsphere:shardingsphere-jdbc` | **`5.5.3`** (unchanged) |

## 3. Add dependencies to your app

### 3.1 Minimum for the sample YAML (`sharding-two-ds.yaml`)

`shardingsphere-jdbc` alone is **not** enough for the example YAML (`mode.repository=Memory` + `HikariDataSource`).

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
    <version>5.5.3</version>
  </dependency>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-standalone-mode-repository-memory</artifactId>
    <version>5.5.3</version>
  </dependency>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-authority-simple</artifactId>
    <version>5.5.3</version>
  </dependency>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-infra-data-source-pool-hikari</artifactId>
    <version>5.5.3</version>
  </dependency>
  <dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
  </dependency>
  <dependency>
    <groupId>com.xugudb.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
    <version>5.5.3-xugu</version>
  </dependency>
  <dependency>
    <groupId>com.xugudb</groupId>
    <artifactId>xugu-jdbc</artifactId>
    <version>12.3.6</version>
  </dependency>
</dependencies>
```

The dialect registers via Java SPI on the classpath — no MySQL trunk storage path. (For **Proxy**, see [proxy-quick-start.md](proxy-quick-start.md).)

### 3.2 Extra modules by capability

| Capability | Additional Maven artifacts (`org.apache.shardingsphere`, `5.5.3`) |
|---|---|
| Encrypt | `shardingsphere-encrypt-core` |
| Readwrite-splitting | `shardingsphere-readwrite-splitting-core` |
| XA (Atomikos) | `shardingsphere-transaction-xa-atomikos` (+ dialect `shardingsphere-transaction-xugu` when using the XuGu XA wrapper SPI) |

## 4. YAML dual-DS example

Copy and edit [`docs/examples/sharding-two-ds.yaml`](examples/sharding-two-ds.yaml):

- Replace `<HOST>`, `<PORT>`, `<DB0>`, `<DB1>`, `<USER>`, `<PASSWORD>`
- Keep **`compatiblemode=NONE`** (and typically `charset=UTF8`) on every JDBC URL
- Physical table nodes use uppercase (`T_ORDER`) because XuGu `IdentifierPatternType=UPPER_CASE`

**Topology:** Create **two different DATABASE** names on XuGu (e.g. `shard_ds0` / `shard_ds1`) and point each data source at one database. Multiple ports on the same host (e.g. `5287` / `5288` / `5289`) are usually **cluster entry points sharing `SYSTEM`** — they are **not** three isolated shards. Do **not** configure three `…/SYSTEM` URLs as sharding data sources.

Load the YAML with ShardingSphere JDBC:

```java
DataSource ds = YamlShardingSphereDataSourceFactory.createDataSource(
    Files.readAllBytes(Path.of("sharding-two-ds.yaml")));
```

## 5. SQL usage notes

- Cross-shard aggregates should use **explicit aliases**, e.g. `SELECT COUNT(*) AS cnt FROM t_order`. Bare `COUNT(*)` may fail at result-merge / column-label binding when shards are merged.
- Prefer reading columns by alias or ordinal after merge.

## 6. IT host defaults (project verification)

`tests-it` defaults (`tests-it/src/test/resources/it-xugu.properties`):

| Property | Default |
|---|---|
| Host / port | `192.168.2.239:5287` |
| User / password | `SYSDBA` / `SYSDBA` |
| Mode | **`compatiblemode=NONE`** only |

Override via the properties file or system properties when your lab differs. If the host is unreachable, ITs use JUnit `Assumptions` and **skip** (not fail).

## 7. Verify

### Option A — project IT (recommended)

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd `
  -pl tests-it -am test -Pit-xugu `
  -Dtest=ShardingCrudIT `
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expect: dual-DS CREATE / INSERT / SELECT / LIMIT / DROP through the XuGu native dialect.

### Option B — your application

Run a minimal CRUD against logical table `t_order` (shard key `user_id`) with `sql-show: true` and confirm statements hit `ds_0` / `ds_1` as expected.

## Scope reminders

- **Supported:** JDBC dialect, **Proxy (MySQL wire → XuGu NONE)**, `compatiblemode=NONE`, sharding CRUD + LIMIT pagination
- **Not in scope:** OSS trunk MySQL Proxy **storage** path (`proxy-backend-mysql` / `proxy-dialect-mysql`), MySQL trunk fallback, other XuGu compatible modes
- Proxy path: [`proxy-quick-start.md`](proxy-quick-start.md) · Capability matrix: [`parity-matrix.md`](parity-matrix.md) · Pagination: [`pagination-decision.md`](pagination-decision.md)
