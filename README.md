# shardingsphere-dialect-xugu

面向 **Apache ShardingSphere 5.5.3** 的 **虚谷（XuGu）原生 JDBC 方言**插件，发布坐标版本 **`5.5.3-xugu`**。

本仓库提供可安装的方言 JAR：在 JDBC 应用中通过 SPI 注册 `DatabaseType = XuGu`，或在 **ShardingSphere-Proxy** 中以 **MySQL 线协议前端** 接入，存储侧走 XuGu 方言栈（**`compatiblemode=NONE` only**）。

| 文档 | 说明 |
|---|---|
| [docs/quick-start.md](docs/quick-start.md) | JDBC 快速上手（约 30 分钟） |
| [docs/proxy-quick-start.md](docs/proxy-quick-start.md) | Proxy：MySQL wire → XuGu NONE 存储 |
| [docs/support-matrix.md](docs/support-matrix.md) | **生产能力白名单**与已知限制 |
| [docs/RELEASE-NOTES-5.5.3-xugu.md](docs/RELEASE-NOTES-5.5.3-xugu.md) | `5.5.3-xugu` 发行说明 |

---

## 产品概览

- **是什么**：ShardingSphere 的 XuGu 数据库类型插件（解析 / 绑定 / 路由 / 改写 / 事务 / 异常映射等 SPI），非上游 MySQL 主干回退方案。
- **适用场景**：分库分表 CRUD、读写分离（同机多 DATABASE 拓扑验证）、本地事务与 Savepoint、LIMIT 分页、加密规则、XA 包装（happy-path）等——以 [support-matrix.md](docs/support-matrix.md) 白名单为准。
- **明确不支持**：其他 XuGu 兼容模式；OSS 主干 Proxy 的 MySQL 存储解析路径；未在矩阵中声明的 SHOW DAL / 权限检查器等 DEFER 项。

```text
# JDBC
App → shardingsphere-jdbc + jdbc-dialect-xugu → jdbc:xugu://…?compatiblemode=NONE

# Proxy
App (MySQL JDBC) --MySQL wire--> Proxy frontend-mysql
                                      └─ XuGu dialect / storage (NONE，非 MySQL trunk backend)
```

---

## Maven 坐标

**GroupId：** `com.xugudb.shardingsphere`  
**Version：** `5.5.3-xugu`  
**上游：** `org.apache.shardingsphere:*:5.5.3`（不变）  
**驱动：** `com.xugudb:xugu-jdbc:12.3.6`（需自行安装到本地 `.m2` 或私服）

### 消费者常用依赖（JDBC）

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
    <version>5.5.3-xugu</version>
  </dependency>
  <dependency>
    <groupId>com.xugudb</groupId>
    <artifactId>xugu-jdbc</artifactId>
    <version>12.3.6</version>
  </dependency>
</dependencies>
```

`shardingsphere-jdbc-dialect-xugu` 会传递依赖 connector / parser / binder / route / rewrite / sharding / federation / transaction / exception 等模块。

### 从哪里获取 JAR

| 渠道 | 说明 |
|---|---|
| **GitHub Release 附件（默认）** | 在 [Releases](https://github.com/payAgain/shardingsphere-dialect-xugu/releases) 下载模块 JAR 或 `shardingsphere-dialect-xugu-5.5.3-xugu-jars.zip`，再安装到 `.m2` 或放入应用 `lib/` / Proxy `ext-lib/` |
| **GitHub Packages** | 父 POM 已配置 `distributionManagement`；需具备 `write:packages` 的 PAT 执行 `mvn -Pgithub-packages deploy`。当前默认发布以 Release 附件为准 |
| **本地构建** | `mvn -DskipITs clean install` 安装到本机 `.m2` |
| **Maven Central** | 未配置 Sonatype；本版本不走 Central |

#### GitHub Packages 仓库块（可选）

若制品已发布到本仓库的 GitHub Packages：

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/payAgain/shardingsphere-dialect-xugu</url>
  </repository>
</repositories>
```

`~/.m2/settings.xml` 中需配置同名 `server`（用户名任意 GitHub 账号，密码为具备 `read:packages` 的 PAT / `GITHUB_TOKEN`）。私有包拉取需要认证。

#### 从 Release 附件安装示例

```powershell
mvn install:install-file `
  -Dfile=shardingsphere-jdbc-dialect-xugu-5.5.3-xugu.jar `
  -DgroupId=com.xugudb.shardingsphere `
  -DartifactId=shardingsphere-jdbc-dialect-xugu `
  -Dversion=5.5.3-xugu `
  -Dpackaging=jar
```

（其他模块 JAR 同理；完整依赖树建议直接使用本仓库 `mvn install` 或 Release 中的模块集合。）

---

## 模块一览

| 模块目录 | ArtifactId | 角色 |
|---|---|---|
| `connector-xugu` | `shardingsphere-database-connector-xugu` | 数据库类型 / 连接元数据 |
| `parser-sql-statement-xugu` | `shardingsphere-parser-sql-statement-xugu` | SQL Statement 模型 |
| `parser-sql-engine-xugu` | `shardingsphere-parser-sql-engine-xugu` | 解析引擎（白名单语法） |
| `infra-binder-xugu` | `shardingsphere-infra-binder-xugu` | 绑定 |
| `infra-route-xugu` | `shardingsphere-infra-route-xugu` | 路由 |
| `infra-rewrite-xugu` | `shardingsphere-infra-rewrite-xugu` | 改写 |
| `sharding-dialect-xugu` | `shardingsphere-sharding-dialect-xugu` | 分片方言 |
| `sql-federation-xugu` | `shardingsphere-sql-federation-xugu` | 联邦桩 |
| `transaction-xugu` | `shardingsphere-transaction-xugu` | Savepoint / XA 包装 |
| `database-exception-xugu` | `shardingsphere-database-exception-xugu` | 异常映射 |
| `jdbc-dialect-xugu` | **`shardingsphere-jdbc-dialect-xugu`** | **JDBC 聚合入口（首选）** |
| `proxy-backend-xugu` | `shardingsphere-proxy-backend-xugu` | Proxy 后端 SPI |
| `proxy-dialect-xugu` | `shardingsphere-proxy-dialect-xugu` | Proxy 依赖聚合（packaging=pom） |
| `tests-it` | `shardingsphere-tests-it-xugu` | 集成测试（不对外发布） |

---

## 快速开始

### 1. 前置条件

- JDK 8+、Maven 3.9+
- 虚谷 JDBC：`xugu-jdbc` **12.3.6**
- 可达的 XuGu 实例；JDBC URL **必须**带 **`compatiblemode=NONE`**

```powershell
mvn install:install-file `
  -Dfile=path\to\xugu-jdbc-12.3.6.jar `
  -DgroupId=com.xugudb `
  -DartifactId=xugu-jdbc `
  -Dversion=12.3.6 `
  -Dpackaging=jar
```

### 2. JDBC 路径

1. 获取方言：本地 `mvn -DskipITs clean install`，或从 [Release](https://github.com/payAgain/shardingsphere-dialect-xugu/releases) / Packages 引入 `shardingsphere-jdbc-dialect-xugu`。
2. 在应用中加入上文依赖片段。
3. 参考示例 YAML：[docs/examples/sharding-two-ds.yaml](docs/examples/sharding-two-ds.yaml)（占位符替换主机/库/账号；保留 `compatiblemode=NONE`）。
4. 细则：[docs/quick-start.md](docs/quick-start.md)。

### 3. Proxy 路径（MySQL wire）

1. 下载并解压 **Apache ShardingSphere-Proxy 5.5.3**。
2. 构建本仓库后，将 `proxy-dialect-xugu` 的 runtime 依赖拷入 Proxy 的 `ext-lib/`（见 [proxy-quick-start.md](docs/proxy-quick-start.md)）。
3. 前端使用 **MySQL** 线协议；存储数据源为 `jdbc:xugu://…?compatiblemode=NONE`。
4. **不要**再放入上游 `proxy-backend-mysql` / MySQL 存储解析引擎作为存储后端。

---

## 白名单与限制（必读）

对外承诺以 **[docs/support-matrix.md](docs/support-matrix.md)** 为准，摘要如下：

| 在范围内 | 不在范围内 / 注意 |
|---|---|
| `getDatabaseType() == "XuGu"` 的 JDBC 方言 SPI | 其他 XuGu `compatiblemode` |
| 分片 CRUD、LIMIT、同机读写分离验证、加密规则等白名单能力 | 未验证的生产 SLA、多机物理副本隔离 |
| Proxy：**MySQL wire** + XuGu NONE 存储 | OSS 主干 MySQL Proxy 存储路径 |
| XA / Savepoint 等矩阵已声明项 | `DialectDatabasePrivilegeChecker`、SHOW DAL 等 DEFER 项 |

未列入白名单的能力，请勿按「已支持」对外宣称。

---

## 构建

```powershell
# 跳过 IT，打包（推荐发布流水线）
mvn -DskipITs clean package

# 安装到本地 .m2
mvn -DskipITs clean install
```

单元测试默认排除 `*IT`；集成测试需 `-Pit-xugu` 等 profile，且依赖实验室 XuGu 可达。

### 发布到 GitHub Packages（维护者）

父 POM 提供 `github-packages` profile：

```powershell
# 需 GITHUB_TOKEN（或 settings.xml 中 id=github 的 PAT，具备 write:packages）
mvn -DskipITs -Pgithub-packages -pl "!tests-it" clean deploy
```

若 Sonatype / Maven Central 未配置，本项目**不以 Central 为默认发布渠道**；消费者以 Release 附件或 GitHub Packages 为准。

---

## 实验室说明

集成测试默认读取 `tests-it/src/test/resources/it-xugu.properties`（及 env2 / Proxy 等变体）。主机与账号属于实验室环境，**请勿将真实口令写入文档或 Issue**。主机不可达时 IT 以 Assumption skip，不视为失败证据。拓扑说明见 [docs/topology-same-host.md](docs/topology-same-host.md)。

---

## 许可证

本仓库当前**未附带独立 LICENSE 文件**。代码基于 Apache ShardingSphere **5.5.3** 方言扩展模式开发；上游 ShardingSphere 采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)。虚谷 JDBC 驱动 `xugu-jdbc` 的许可与分发以其厂商条款为准，不随本仓库重新授权。
