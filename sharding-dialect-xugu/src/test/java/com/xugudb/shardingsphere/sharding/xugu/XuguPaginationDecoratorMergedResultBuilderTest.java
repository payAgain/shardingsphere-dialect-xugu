package com.xugudb.shardingsphere.sharding.xugu;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.binder.context.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.merge.result.impl.decorator.DecoratorMergedResult;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sharding.merge.dql.pagination.LimitDecoratorMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.builder.PaginationDecoratorMergedResultBuilder;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

class XuguPaginationDecoratorMergedResultBuilderTest {
    
    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");
    
    private final PaginationDecoratorMergedResultBuilder builder =
            DatabaseTypedSPILoader.getService(PaginationDecoratorMergedResultBuilder.class, databaseType);
    
    @Test
    void assertDatabaseType() {
        assertThat(builder.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertBuildReturnsLimitDecoratorMergedResult() throws SQLException {
        PaginationContext paginationContext = new PaginationContext(null, null, Collections.emptyList());
        DecoratorMergedResult actual = builder.build(mock(MergedResult.class), paginationContext);
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
    }
}
