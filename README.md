# shardingsphere-dialect-xugu

Apache ShardingSphere XuGu native JDBC dialect plugin. **Only `compatiblemode=NONE` is supported** for integration tests and production use documented here. The project depends on **ShardingSphere 5.5.3** (`org.apache.shardingsphere:*:5.5.3`). Integration test database host, port, and credentials are configurable via `tests-it` properties (see Task 4+).

Install the XuGu JDBC driver locally before building:

```powershell
mvn install:install-file -Dfile=path\to\xugu-jdbc-12.3.6.jar -DgroupId=com.xugudb -DartifactId=xugu-jdbc -Dversion=12.3.6 -Dpackaging=jar
```

Validate the multi-module skeleton:

```powershell
mvn -q validate
```
