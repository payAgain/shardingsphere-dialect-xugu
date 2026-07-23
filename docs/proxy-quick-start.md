# Proxy Quick Start — XuGu dialect `5.5.3-xugu`

Goal: run **Apache ShardingSphere-Proxy 5.5.3** with the XuGu dialect aggregate, accepting **MySQL wire** clients while storing data in **XuGu `compatiblemode=NONE`**.

```text
App (MySQL JDBC) --MySQL wire--> Proxy frontend-mysql (+ MySQL client SQL parse)
                                      |
                                      +-- XuGu dialect stack (NONE storage parser/binder/route)
                                      +-- proxy-backend-xugu (NOT proxy-backend-mysql)
                                      +-- storage jdbc:xugu://…?compatiblemode=NONE
```

## Prerequisites

- JDK 8+
- Maven 3.9+ (build dialect jars)
- **Apache ShardingSphere-Proxy 5.5.3** binary distribution  
  Download: [https://shardingsphere.apache.org/document/5.5.3/en/downloads/](https://shardingsphere.apache.org/document/5.5.3/en/downloads/)  
  Extract to e.g. `C:\tools\apache-shardingsphere-5.5.3-shardingsphere-proxy-bin`
- XuGu JDBC driver `xugu-jdbc` **12.3.6** (installed to local `.m2` or copied as JAR; use **explicit** `12.3.6` GAV)
- Reachable XuGu instance (lab default below)

## 1. Install XuGu JDBC into local `.m2` (if not already)

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file `
  "-Dfile=path\to\xugu-jdbc-12.3.6.jar" `
  "-DgroupId=com.xugudb" `
  "-DartifactId=xugu-jdbc" `
  "-Dversion=12.3.6" `
  "-Dpackaging=jar" `
  "-DgeneratePom=true"
```

## 2. Build & install dialect (including proxy aggregate)

From the dialect repo root:

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\pom.xml" `
  -pl proxy-dialect-xugu -am `
  clean install "-DskipITs"
```

Key coordinates (version **`5.5.3-xugu`**):

| Artifact | Role |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-proxy-dialect-xugu` | Aggregate POM — dependency BOM for ext-lib |
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | XuGu **storage** parser / binder / route stack |
| `com.xugudb.shardingsphere:shardingsphere-proxy-backend-xugu` | Proxy **storage** backend SPI for XuGu |
| `org.apache.shardingsphere:shardingsphere-proxy-frontend-mysql:5.5.3` | MySQL **wire** frontend (exclude `proxy-backend-mysql`) |
| `org.apache.shardingsphere:shardingsphere-protocol-mysql:5.5.3` | MySQL protocol codec (upstream) |
| `org.apache.shardingsphere:shardingsphere-proxy-frontend-core:5.5.3` | Shared frontend core (upstream) |

### Wire vs storage (read carefully)

| Layer | Allowed | Forbidden |
|---|---|---|
| **Client wire / frontend SQL** | `proxy-frontend-mysql` + `protocol-mysql`. On SS **5.5.3**, the MySQL frontend typically also requires **`parser-sql-engine-mysql`** on the classpath to parse client SQL (`DialectSQLParserFacade`). That is **wire-side**, not XuGu storage. | Treating MySQL parser presence as “storage = MySQL trunk” |
| **Storage dialect / backend** | `jdbc-dialect-xugu` → `parser-sql-engine-xugu` + `proxy-backend-xugu` | `proxy-backend-mysql`, `proxy-dialect-mysql` (would swap **storage** parsing/backend to MySQL trunk) |

## 3. Copy jars into Proxy `ext-lib/`

In your Proxy distribution directory:

```powershell
$PROXY_HOME = "C:\tools\apache-shardingsphere-5.5.3-shardingsphere-proxy-bin"
$M2 = "$env:USERPROFILE\.m2\repository"

# Resolve aggregate runtime classpath into ext-lib
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\proxy-dialect-xugu\pom.xml" `
  dependency:copy-dependencies `
  "-DincludeScope=runtime" `
  "-DoutputDirectory=$PROXY_HOME\ext-lib" `
  "-DoverWriteReleases=true" `
  "-DoverWriteSnapshots=true"
```

Also ensure `xugu-jdbc-12.3.6.jar` is present under `ext-lib/` if not pulled transitively.

> **Tip:** Keep **one** wire frontend (MySQL). Do **not** add upstream `shardingsphere-proxy-dialect-mysql` — that replaces the storage dialect stack with MySQL trunk. Excluding `proxy-backend-mysql` from `frontend-mysql` is required so storage stays XuGu.

If startup fails with `DialectSQLParserFacade type null` for the MySQL frontend, ensure `shardingsphere-parser-sql-engine-mysql` is present for **frontend** parsing (via `frontend-mysql` dependency tree or an explicit copy). This does **not** mean storage SQL uses the MySQL trunk parser.

## 4. Configure Proxy

Copy sample YAML from this repo:

| Source (repo) | Target (Proxy) |
|---|---|
| `proxy-dialect-xugu/src/main/resources/conf/server.yaml` | `$PROXY_HOME/conf/server.yaml` |
| `proxy-dialect-xugu/src/main/resources/conf/config-sharding.yaml` | `$PROXY_HOME/conf/config-sharding.yaml` |

Edit `config-sharding.yaml`:

- Replace `<DB0>` and `<DB1>` with **existing, distinct** XuGu DATABASE names (e.g. `shard_ds0`, `shard_ds1`)
- Keep **`compatiblemode=NONE`** on every JDBC URL
- Lab host defaults: `192.168.2.239:5287` / `SYSDBA` (see `tests-it/src/test/resources/it-xugu.properties`; siblings `5288`/`5289` are usually the **same cluster**, not three physical shards)

Create physical `T_ORDER` tables on both databases before first client query.

## 5. Start Proxy

```powershell
cd "$PROXY_HOME\bin"
.\start.bat 3307 conf
```

Default listen port **3307** (MySQL wire). Check `$PROXY_HOME/logs/stdout.log` for startup errors.

## 6. Connect with MySQL client

```powershell
# Example with mysql CLI (if installed)
mysql -h 127.0.0.1 -P 3307 -u sharding -psharding logic_db

# Or MySQL JDBC from application code
# jdbc:mysql://127.0.0.1:3307/logic_db?useSSL=false&characterEncoding=utf-8
```

Run a smoke query:

```sql
SELECT 1;
```

For sharding CRUD against `t_order`, use `user_id` values that route to `ds_0` / `ds_1` per the INLINE algorithm in `config-sharding.yaml`.

Cross-shard aggregates: prefer explicit aliases (`SELECT COUNT(*) AS cnt …`).

## Architecture notes

| Layer | Component | Notes |
|---|---|---|
| Client wire | `shardingsphere-proxy-frontend-mysql` + `shardingsphere-protocol-mysql` | MySQL protocol; frontend may need `parser-sql-engine-mysql` |
| Storage dialect | `shardingsphere-jdbc-dialect-xugu` → `parser-sql-engine-xugu` | **NONE** native XuGu SQL |
| Proxy backend | `shardingsphere-proxy-backend-xugu` | QueryHeader / admin stubs |
| Forbidden (storage) | `shardingsphere-proxy-backend-mysql`, `shardingsphere-proxy-dialect-mysql` | Must not own storage SPI |

## Verify dependency shape (optional)

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\proxy-dialect-xugu\pom.xml" `
  dependency:tree "-Dincludes=com.xugudb.shardingsphere:*,org.apache.shardingsphere:shardingsphere-parser-sql-engine-*,org.apache.shardingsphere:shardingsphere-proxy-backend-*"
```

Expect:

- `shardingsphere-parser-sql-engine-xugu` present (storage)
- `shardingsphere-proxy-backend-xugu` present; **no** `shardingsphere-proxy-backend-mysql`
- `parser-sql-engine-mysql` may appear via the MySQL **frontend** tree — that is expected for wire parsing on 5.5.3

## Integration test (`-Pproxy`)

Embedded Proxy IT (no external Proxy distribution required): MySQL JDBC → in-process Proxy (MySQL wire) → XuGu `compatiblemode=NONE` sharded CRUD.

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\pom.xml" `
  -pl tests-it -am `
  "-Pproxy" test
```

- Includes: `tests-it/src/test/java/**/proxy/**/*IT.java`
- Approach: `BootstrapInitializer` + `ShardingSphereProxy` (SS 5.5.3), conf rendered under a temp directory
- Lab down / XuGu port refused → tests are skipped via `assumeReachable` (evidence status **BLOCKED_ENV**, not PASS)
- Baseline profile remains separate: `"-Pbaseline"` (does not include proxy ITs)

## Version lock

This quick-start targets dialect release **`5.5.3-xugu`** against upstream Proxy **`5.5.3`**. Do not mix other SS minor versions without re-validation.
