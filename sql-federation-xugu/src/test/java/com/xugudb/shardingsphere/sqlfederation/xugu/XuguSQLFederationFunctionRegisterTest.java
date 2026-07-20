package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.tools.Frameworks;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sqlfederation.compiler.sql.function.DialectSQLFederationFunctionRegister;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class XuguSQLFederationFunctionRegisterTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectSQLFederationFunctionRegister register =
            DatabaseTypedSPILoader.getService(DialectSQLFederationFunctionRegister.class, databaseType);
    
    @Test
    void assertSPILoads() {
        assertThat(register, isA(XuguSQLFederationFunctionRegister.class));
        assertThat(register.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertRegisterFunction() {
        assertDoesNotThrow(() -> register.registerFunction(Frameworks.createRootSchema(true), "schema"));
    }
}
