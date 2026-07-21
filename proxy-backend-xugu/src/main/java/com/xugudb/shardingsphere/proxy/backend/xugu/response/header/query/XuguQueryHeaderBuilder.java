package com.xugudb.shardingsphere.proxy.backend.xugu.response.header.query;

import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeaderBuilder;

import java.sql.SQLException;

/**
 * Query header builder for XuGu.
 */
public final class XuguQueryHeaderBuilder implements QueryHeaderBuilder {

    private static final int UNUSED_INT_FIELD = 0;

    private static final String UNUSED_STRING_FIELD = "";

    private static final boolean UNUSED_BOOLEAN_FIELD = false;

    @Override
    public QueryHeader build(final QueryResultMetaData queryResultMetaData, final ShardingSphereDatabase database,
                             final String columnName, final String columnLabel, final int columnIndex) throws SQLException {
        int columnType = queryResultMetaData.getColumnType(columnIndex);
        String columnTypeName = queryResultMetaData.getColumnTypeName(columnIndex);
        int columnLength = queryResultMetaData.getColumnLength(columnIndex);
        return new QueryHeader(UNUSED_STRING_FIELD, UNUSED_STRING_FIELD, columnLabel, UNUSED_STRING_FIELD, columnType, columnTypeName, columnLength,
                UNUSED_INT_FIELD, UNUSED_BOOLEAN_FIELD, UNUSED_BOOLEAN_FIELD, UNUSED_BOOLEAN_FIELD, UNUSED_BOOLEAN_FIELD);
    }

    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
