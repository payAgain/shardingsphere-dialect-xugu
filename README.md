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

## Verification

Unit / module tests:

```powershell
mvn -q test
```

Default `mvn test` runs unit tests only (Surefire excludes `**/*IT.java` and `**/it/**`). Live XuGu IT (host from `tests-it/src/test/resources/it-xugu.properties`; skips if unreachable):

```powershell
mvn -pl tests-it -am test -Pit-xugu
# or a single IT:
mvn -pl tests-it -am test -Pit-xugu -Dtest=NativeCrudIT -Dsurefire.failIfNoSpecifiedTests=false
```

`NativeCrudIT` creates a ShardingSphere-JDBC DataSource (single DS, `compatiblemode=NONE`) and runs CREATE / INSERT / SELECT / UPDATE / DELETE / DROP through the XuGu native dialect SPI.
