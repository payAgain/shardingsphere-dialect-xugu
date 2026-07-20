package com.xugudb.shardingsphere.infra.binder.xugu.bind;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.binder.engine.DialectSQLBindEngine;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XuguSQLBindEngineTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectSQLBindEngine bindEngine =
            DatabaseTypedSPILoader.getService(DialectSQLBindEngine.class, databaseType);
    
    @Test
    void assertDatabaseType() {
        assertThat(bindEngine.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertBindSelectStatementReturnsEmpty() {
        SelectStatement selectStatement = new SelectStatement(databaseType);
        assertFalse(bindEngine.bind(selectStatement, null).isPresent());
    }
}
