package com.xugudb.shardingsphere.transaction.xugu.xa;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.transaction.xa.jta.connection.XAConnectionWrapper;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class XuguXAConnectionWrapperTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final XAConnectionWrapper wrapper = DatabaseTypedSPILoader.getService(XAConnectionWrapper.class, databaseType);
    
    @Test
    void assertSPILoads() {
        assertThat(wrapper, isA(XuguXAConnectionWrapper.class));
        assertThat(wrapper.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertInitLoadsReflection() {
        assertDoesNotThrow(() -> wrapper.init(new Properties()));
    }
}