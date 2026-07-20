package com.xugudb.shardingsphere.database.connector.xugu.jdbcurl;

import org.apache.shardingsphere.database.connector.core.jdbcurl.DialectDefaultQueryPropertiesProvider;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class XuguDefaultQueryPropertiesProviderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectDefaultQueryPropertiesProvider provider =
            DatabaseTypedSPILoader.getService(DialectDefaultQueryPropertiesProvider.class, databaseType);
    
    @Test
    void assertGetDefaultQueryProperties() {
        Properties actual = provider.getDefaultQueryProperties();
        assertThat(actual.getProperty("compatiblemode"), is("NONE"));
        assertThat(actual.getProperty("charset"), is("UTF8"));
    }
}
