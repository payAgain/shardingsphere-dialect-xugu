## 摘要

Apache ShardingSphere **5.5.3** 的虚谷（XuGu）原生方言插件正式发布，版本坐标 **`5.5.3-xugu`**。

- JDBC：通过 SPI 使用 `DatabaseType = XuGu`
- Proxy：**MySQL 线协议前端** + XuGu 存储方言（**非** OSS 主干 MySQL 存储路径）
- 兼容模式：**仅 `compatiblemode=NONE`**
- 生产能力范围以白名单为准，见仓库 `docs/support-matrix.md`
- 工程流程：本仓库由 **Trellis**（`.trellis/`）托管

## Maven 坐标

| 项 | 值 |
|---|---|
| GroupId | `com.xugudb.shardingsphere` |
| 推荐 Artifact | `shardingsphere-jdbc-dialect-xugu` |
| Version | `5.5.3-xugu` |
| 上游 | `org.apache.shardingsphere:*:5.5.3` |
| 驱动 | `com.xugudb:xugu-jdbc:12.3.6`（自行安装；**显式坐标**，勿依赖 JAR 内嵌 POM） |

```xml
<dependency>
  <groupId>com.xugudb.shardingsphere</groupId>
  <artifactId>shardingsphere-jdbc-dialect-xugu</artifactId>
  <version>5.5.3-xugu</version>
</dependency>
```

最小可跑 YAML 分片示例时，除上述方言外通常还需：`shardingsphere-jdbc`、`standalone-mode-repository-memory`、`authority-simple`、`infra-data-source-pool-hikari`、`HikariCP`。详见 `docs/quick-start.md`。

## 白名单范围（摘要）

**支持（受控生产评估）：** 分片 CRUD、LIMIT 分页、同机读写分离验证、本地事务/Savepoint、加密规则、XA 包装（happy-path）、Proxy（MySQL wire → XuGu NONE）等——详见 support-matrix。

**不支持 / DEFER：** 其他 `compatiblemode`；OSS 主干 MySQL Proxy **存储**路径（`proxy-backend-mysql` / `proxy-dialect-mysql`）；`DialectDatabasePrivilegeChecker`、SHOW DAL 等未声明项；未验证的多机物理副本 SLA。

## Proxy 说明

客户端走 **MySQL wire**（`frontend-mysql` + `protocol-mysql`）。SS 5.5.3 前端解析客户端 SQL 时 classpath 上通常会出现上游 `parser-sql-engine-mysql`（**仅 wire/前端**）。存储 JDBC 为 `jdbc:xugu://…?compatiblemode=NONE`，使用本仓库 `proxy-backend-xugu` + JDBC 方言栈。

**禁止**再叠加上游 `proxy-backend-mysql` 或 `proxy-dialect-mysql` 作为存储后端。快速开始见 `docs/proxy-quick-start.md`。

## 制品获取

本 Release **Assets** 提供：

1. **`shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip`** — 含：
   - **`shardingsphere-dialect-xugu-parent-5.5.3-xugu.pom`**（模块 POM 的 parent，**安装必装**）
   - 各模块 JAR + 对应模块 POM
   - `shardingsphere-proxy-dialect-xugu-5.5.3-xugu.pom` 聚合 POM
2. 各模块独立 JAR（便于单独下载）

**发布渠道：** 默认以本页附件为准。父 POM 已配置 GitHub Packages `distributionManagement`，但当前 CI token 无 `write:packages`，**未**推送到 Packages / Maven Central。本地也可：`mvn -DskipITs clean install`。

### 推荐安装（解压 ZIP 后）

仓库脚本（克隆本仓库时）：

```powershell
# 先打包（需已 mvn package）
.\scripts\package-release-assets.ps1

# 安装到本地 .m2（JdbcJar 换成你的驱动路径）
.\scripts\install-release-assets.ps1 `
  -ZipPath .\dist\shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip `
  -JdbcJar path\to\xugu-jdbc-12.3.6.jar
```

手动顺序（务必遵守）：

1. `install-file` parent POM（`…-parent-5.5.3-xugu.pom`）
2. JDBC：`-DgroupId=com.xugudb -DartifactId=xugu-jdbc -Dversion=12.3.6 -DgeneratePom=true`（驱动 JAR 内嵌 POM 可能仍写 `12.3.4`）
3. 各模块：`-Dfile=….jar -DpomFile=….pom`
4. `proxy-dialect-xugu` 聚合 POM

**不要**只对单个 dialect JAR 执行一次 `install-file` 就期望传递依赖完整解析。

## 使用注意

- 跨分片聚合请显式别名，例如 `SELECT COUNT(*) AS cnt FROM t_order`（勿依赖匿名列标签）。
- 多端口（如 `5287/5288/5289`）是同一 XuGu 集群入口，共享 `SYSTEM`；分片必须使用**不同 DATABASE**，不能把三个 `…/SYSTEM` URL 当分片。

## 文档

- 中文 README：`README.md`
- 支持矩阵：`docs/support-matrix.md`
- JDBC 快速开始：`docs/quick-start.md`
- Proxy 快速开始：`docs/proxy-quick-start.md`
- 发行说明：`docs/RELEASE-NOTES-5.5.3-xugu.md`
