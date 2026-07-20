package com.xugudb.shardingsphere.infra.route.xugu;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.route.engine.tableless.DialectDALStatementBroadcastRouteDecider;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.DALStatement;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class XuguDALStatementBroadcastRouteDeciderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectDALStatementBroadcastRouteDecider decider =
            DatabaseTypedSPILoader.getService(DialectDALStatementBroadcastRouteDecider.class, databaseType);
    
    @Test
    void assertDatabaseType() {
        assertThat(decider.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertIsDataSourceBroadcastRouteAlwaysFalse() {
        assertFalse(decider.isDataSourceBroadcastRoute(mock(DALStatement.class)));
    }
    
    @Test
    void assertIsInstanceBroadcastRouteAlwaysFalse() {
        assertFalse(decider.isInstanceBroadcastRoute(mock(DALStatement.class)));
    }
}
