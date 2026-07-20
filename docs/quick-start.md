# Quick Start (≈30 minutes) — ShardingSphere XuGu dialect `5.5.3-xugu.2`

Goal: install the dialect into the local Maven repo, wire a dual-datasource sharding app (or run the project IT), and verify CRUD under **`compatiblemode=NONE` only**.

## Prerequisites

- JDK 8+ (ANTLR parser modules need a toolchain that can compile the grammar; project targets Java 8 bytecode)
- Maven 3.9+
- XuGu JDBC driver JAR `xugu-jdbc` **12.3.6**
- Reachable XuGu instance (IT defaults below)

## 1. Install XuGu JDBC into local `.m2`

```powershell
mvn install:install-file `
  -Dfile=path\to\xugu-jdbc-12.3.6.jar `
  -DgroupId=com.xugudb `
  -DartifactId=xugu-jdbc `
  -Dversion=12.3.6 `
  -Dpackaging=jar
```

## 2. Build & install this dialect (release coordinates)

From the repo root:

```powershell
C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd -q clean install
```

Published locally (among others):

| Coordinate | Version |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | **`5.5.3-xugu.2`** |
| Upstream `org.apache.shardingsphere:shardingsphere-jdbc` | **`5.5.3`** (unchanged) |

## 3. Add dependencies to your app

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
    <version>5.5.3</version>
  </dependency>
  <dependency>
    <groupId>com.xugudb.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
    <version>5.5.3-xugu.2</version>
  </dependency>
  <dependency>
    <groupId>com.xugudb</groupId>
    <artifactId>xugu-jdbc</artifactId>
    <version>12.3.6</version>
  </dependency>
</dependencies>
```

The dialect registers via Java SPI on the classpath — no MySQL trunk, no Proxy module.

## 4. YAML dual-DS example

Copy and edit [`docs/examples/sharding-two-ds.yaml`](examples/sharding-two-ds.yaml):

- Replace `<HOST>`, `<PORT>`, `<DB0>`, `<DB1>`, `<USER>`, `<PASSWORD>`
- Keep **`compatiblemode=NONE`** (and typically `charset=UTF8`) on every JDBC URL
- Physical table nodes use uppercase (`T_ORDER`) because XuGu `IdentifierPatternType=UPPER_CASE`

Create two databases (or schemas) on XuGu before first run, e.g. `shard_ds0` / `shard_ds1`, then point each data source URL at one database.

Load the YAML with ShardingSphere JDBC:

```java
DataSource ds = YamlShardingSphereDataSourceFactory.createDataSource(
    Files.readAllBytes(Path.of("sharding-two-ds.yaml")));
```

## 5. IT host defaults (project verification)

`tests-it` defaults (`tests-it/src/test/resources/it-xugu.properties`):

| Property | Default |
|---|---|
| Host / port | `192.168.2.239:5138` |
| User / password | `SYSDBA` / `SYSDBA` |
| Mode | **`compatiblemode=NONE`** only |

Override via the properties file or system properties when your lab differs. If the host is unreachable, ITs use JUnit `Assumptions` and **skip** (not fail).

## 6. Verify

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

- **Supported:** JDBC dialect, `compatiblemode=NONE`, sharding CRUD + LIMIT pagination
- **Not in scope:** ShardingSphere Proxy, MySQL trunk fallback, other XuGu compatible modes
- Capability matrix: [`parity-matrix.md`](parity-matrix.md) · Pagination: [`pagination-decision.md`](pagination-decision.md)
