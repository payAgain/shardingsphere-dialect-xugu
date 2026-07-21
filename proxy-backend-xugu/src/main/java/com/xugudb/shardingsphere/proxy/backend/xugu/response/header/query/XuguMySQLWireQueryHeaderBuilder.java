package com.xugudb.shardingsphere.proxy.backend.xugu.response.header.query;

import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeaderBuilder;

import java.sql.SQLException;

/**
 * Query header builder registered for MySQL <em>wire</em> protocol type.
 *
 * <p>Proxy builds admin/query response headers with the frontend protocol type ({@code MySQL}).
 * Without a MySQL-typed {@link QueryHeaderBuilder}, Connector/J handshake fails after
 * {@code SELECT @@...} is answered. Storage remains XuGu — this is not {@code proxy-backend-mysql}.</p>
 */
public final class XuguMySQLWireQueryHeaderBuilder implements QueryHeaderBuilder {

    private final XuguQueryHeaderBuilder delegate = new XuguQueryHeaderBuilder();

    @Override
    public QueryHeader build(final QueryResultMetaData queryResultMetaData, final ShardingSphereDatabase database,
                             final String columnName, final String columnLabel, final int columnIndex) throws SQLException {
        return delegate.build(queryResultMetaData, database, columnName, columnLabel, columnIndex);
    }

    @Override
    public String getDatabaseType() {
        return "MySQL";
    }
}
