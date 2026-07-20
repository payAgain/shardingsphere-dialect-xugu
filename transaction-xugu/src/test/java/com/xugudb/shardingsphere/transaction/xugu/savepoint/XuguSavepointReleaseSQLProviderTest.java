package com.xugudb.shardingsphere.transaction.xugu.savepoint;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.transaction.savepoint.SavepointReleaseSQLProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

class XuguSavepointReleaseSQLProviderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final SavepointReleaseSQLProvider provider =
            DatabaseTypedSPILoader.getService(SavepointReleaseSQLProvider.class, databaseType);
    
    @Test
    void assertSPILoads() {
        assertThat(provider, isA(XuguSavepointReleaseSQLProvider.class));
        assertThat(provider.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertGetSQL() {
        assertThat(provider.getSQL("sp1"), is("RELEASE SAVEPOINT sp1"));
    }
}
