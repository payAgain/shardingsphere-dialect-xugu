package com.xugudb.shardingsphere.database.connector.xugu.type;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XuguDatabaseTypeTest {

    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    @Test
    void assertGetJdbcUrlPrefixes() {
        assertThat(databaseType.getJdbcUrlPrefixes(), is(Collections.singleton("jdbc:xugu:")));
    }

    @Test
    void assertNoTrunkDatabaseType() {
        assertFalse(databaseType.getTrunkDatabaseType().isPresent());
    }
}
