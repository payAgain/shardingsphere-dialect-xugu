package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.system;

import org.apache.shardingsphere.database.connector.core.metadata.database.system.DialectKernelSupportedSystemTable;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

class XuguKernelSupportedSystemTableTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectKernelSupportedSystemTable kernelSupportedSystemTable =
            DatabaseTypedSPILoader.getService(DialectKernelSupportedSystemTable.class, databaseType);
    
    @Test
    void assertGetSchemaAndTablesMap() {
        Map<String, Collection<String>> actual = kernelSupportedSystemTable.getSchemaAndTablesMap();
        assertThat(actual.size(), is(1));
        assertThat(actual.keySet().iterator().next(), is("sysdba"));
        assertThat(actual.get("sysdba"), hasItem("ALL_TABLES"));
        assertThat(actual.get("sysdba"), hasItem("ALL_COLUMNS"));
        assertThat(actual.get("sysdba"), hasItem("ALL_INDEXES"));
        assertThat(actual.get("sysdba"), hasItem("ALL_CONSTRAINTS"));
        assertThat(actual.get("sysdba"), hasItem("ALL_VIEWS"));
    }
}
