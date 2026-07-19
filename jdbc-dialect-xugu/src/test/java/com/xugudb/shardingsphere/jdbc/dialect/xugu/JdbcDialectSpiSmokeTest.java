package com.xugudb.shardingsphere.jdbc.dialect.xugu;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JdbcDialectSpiSmokeTest {

    @Test
    void loadsXuGuDatabaseTypeFromClasspath() {
        assertNotNull(TypedSPILoader.getService(DatabaseType.class, "XuGu"));
    }
}