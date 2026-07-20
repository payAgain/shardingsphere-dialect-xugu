package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sqlfederation.compiler.context.connection.config.DialectSQLFederationConnectionConfigBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XuguSQLFederationConnectionConfigBuilderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectSQLFederationConnectionConfigBuilder builder =
            DatabaseTypedSPILoader.getService(DialectSQLFederationConnectionConfigBuilder.class, databaseType);
    
    @Test
    void assertDatabaseType() {
        assertThat(builder.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertBuild() {
        CalciteConnectionConfig actualConfig = builder.build();
        assertNotNull(actualConfig);
        assertThat(actualConfig.lex(), is(Lex.ORACLE));
        assertThat(actualConfig.conformance(), is(SqlConformanceEnum.ORACLE_12));
        assertNotNull(actualConfig.fun(SqlOperatorTable.class, null));
        assertThat(actualConfig.caseSensitive(), is(Lex.ORACLE.caseSensitive));
    }
}
