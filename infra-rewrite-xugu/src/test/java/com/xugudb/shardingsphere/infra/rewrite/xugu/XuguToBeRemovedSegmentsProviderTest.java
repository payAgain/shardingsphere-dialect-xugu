package com.xugudb.shardingsphere.infra.rewrite.xugu;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.generator.generic.DialectToBeRemovedSegmentsProvider;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

class XuguToBeRemovedSegmentsProviderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final DialectToBeRemovedSegmentsProvider provider =
            DatabaseTypedSPILoader.getService(DialectToBeRemovedSegmentsProvider.class, databaseType);
    
    @Test
    void assertDatabaseType() {
        assertThat(provider.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertGetToBeRemovedSQLSegmentsAlwaysEmpty() {
        assertThat(provider.getToBeRemovedSQLSegments(mock(SQLStatement.class)), empty());
    }
}
