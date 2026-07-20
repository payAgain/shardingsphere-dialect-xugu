package com.xugudb.shardingsphere.sharding.xugu;

import org.apache.shardingsphere.infra.binder.context.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.merge.result.impl.decorator.DecoratorMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.LimitDecoratorMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.builder.PaginationDecoratorMergedResultBuilder;

import java.sql.SQLException;

/**
 * Pagination decorator merged result builder for XuGu (LIMIT strategy).
 */
public final class XuguPaginationDecoratorMergedResultBuilder implements PaginationDecoratorMergedResultBuilder {
    
    @Override
    public DecoratorMergedResult build(final MergedResult mergedResult, final PaginationContext paginationContext) throws SQLException {
        return new LimitDecoratorMergedResult(mergedResult, paginationContext);
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
