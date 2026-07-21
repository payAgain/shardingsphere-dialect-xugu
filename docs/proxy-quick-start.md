# Proxy Quick Start — XuGu dialect `5.5.3-xugu.2`

Goal: run **Apache ShardingSphere-Proxy 5.5.3** with the XuGu dialect aggregate, accepting **MySQL wire** clients while storing data in **XuGu `compatiblemode=NONE`**.

```text
App (MySQL JDBC) --MySQL wire--> Proxy frontend-mysql
                                      |
                                      +-- XuGu dialect stack (NONE, no MySQL trunk parser)
                                      +-- storage jdbc:xugu://…?compatiblemode=NONE
```

## Prerequisites

- JDK 8+
- Maven 3.9+ (build dialect jars)
- **Apache ShardingSphere-Proxy 5.5.3** binary distribution  
  Download: [https://shardingsphere.apache.org/document/5.5.3/en/downloads/](https://shardingsphere.apache.org/document/5.5.3/en/downloads/)  
  Extract to e.g. `C:\tools\apache-shardingsphere-5.5.3-shardingsphere-proxy-bin`
- XuGu JDBC driver `xugu-jdbc` **12.3.6** (installed to local `.m2` or copied as JAR)
- Reachable XuGu instance (lab default below)

## 1. Install XuGu JDBC into local `.m2` (if not already)

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" install:install-file `
  "-Dfile=path\to\xugu-jdbc-12.3.6.jar" `
  "-DgroupId=com.xugudb" `
  "-DartifactId=xugu-jdbc" `
  "-Dversion=12.3.6" `
  "-Dpackaging=jar"
```

## 2. Build & install dialect (including proxy aggregate)

From the dialect repo root:

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\pom.xml" `
  -pl proxy-dialect-xugu -am `
  clean install "-DskipITs"
```

Key coordinates (version **`5.5.3-xugu.2`**):

| Artifact | Role |
|---|---|
| `com.xugudb.shardingsphere:shardingsphere-proxy-dialect-xugu` | Aggregate POM — dependency BOM for ext-lib |
| `com.xugudb.shardingsphere:shardingsphere-jdbc-dialect-xugu` | XuGu storage parser / binder / route stack |
| `com.xugudb.shardingsphere:shardingsphere-proxy-backend-xugu` | Proxy backend SPI for XuGu |
| `org.apache.shardingsphere:shardingsphere-proxy-frontend-mysql:5.5.3` | MySQL wire frontend (upstream; **excludes** `proxy-backend-mysql`) |
| `org.apache.shardingsphere:shardingsphere-protocol-mysql:5.5.3` | MySQL protocol codec (upstream) |
| `org.apache.shardingsphere:shardingsphere-proxy-frontend-core:5.5.3` | Shared frontend core (upstream) |

**Not included:** `shardingsphere-parser-sql-engine-mysql` or `shardingsphere-proxy-backend-mysql` — storage SQL parsing and Proxy backend SPI stay on XuGu modules.

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

> **Tip:** Only place **one** frontend dialect on `ext-lib` (here: MySQL). Do not add `shardingsphere-proxy-dialect-mysql` from upstream — that would swap storage parsing to MySQL trunk.

## 4. Configure Proxy

Copy sample YAML from this repo:

| Source (repo) | Target (Proxy) |
|---|---|
| `proxy-dialect-xugu/src/main/resources/conf/server.yaml` | `$PROXY_HOME/conf/server.yaml` |
| `proxy-dialect-xugu/src/main/resources/conf/config-sharding.yaml` | `$PROXY_HOME/conf/config-sharding.yaml` |

Edit `config-sharding.yaml`:

- Replace `<DB0>` and `<DB1>` with existing XuGu DATABASE names (e.g. `shard_ds0`, `shard_ds1`)
- Keep **`compatiblemode=NONE`** on every JDBC URL
- Lab host defaults: `192.168.2.239:5138` / `SYSDBA` (see `tests-it/src/test/resources/it-xugu.properties`)

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

## Architecture notes

| Layer | Component | Notes |
|---|---|---|
| Client wire | `shardingsphere-proxy-frontend-mysql` + `shardingsphere-protocol-mysql` | MySQL protocol only |
| Storage dialect | `shardingsphere-jdbc-dialect-xugu` → `parser-sql-engine-xugu` | **NONE** native XuGu SQL |
| Proxy backend | `shardingsphere-proxy-backend-xugu` | QueryHeader / admin stubs |
| Forbidden | `shardingsphere-parser-sql-engine-mysql` | Must not be on ext-lib for XuGu storage |

## Verify dependency shape (optional)

```powershell
& "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd" `
  -f "E:\Work\java\shardingsphere-dialect-xugu\proxy-dialect-xugu\pom.xml" `
  dependency:tree "-Dincludes=com.xugudb.shardingsphere:*,org.apache.shardingsphere:shardingsphere-parser-sql-engine-*"
```

Expect `shardingsphere-parser-sql-engine-xugu` and **no** `shardingsphere-parser-sql-engine-mysql`.

## Version lock

This quick-start targets dialect release **`5.5.3-xugu.2`** against upstream Proxy **`5.5.3`**. Do not mix other SS minor versions without re-validation.
