package com.xugudb.shardingsphere.proxy.backend.xugu.connector.jdbc.statement;

import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.proxy.backend.connector.jdbc.statement.StatementMemoryStrictlyFetchSizeSetter;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Statement memory strictly fetch size setter for XuGu.
 */
public final class XuguStatementMemoryStrictlyFetchSizeSetter implements StatementMemoryStrictlyFetchSizeSetter {

    @Override
    public void setFetchSize(final Statement statement) throws SQLException {
        int configuredFetchSize = ProxyContext.getInstance()
                .getContextManager().getMetaDataContexts().getMetaData().getProps().<Integer>getValue(ConfigurationPropertyKey.PROXY_BACKEND_QUERY_FETCH_SIZE);
        statement.setFetchSize(ConfigurationPropertyKey.PROXY_BACKEND_QUERY_FETCH_SIZE.getDefaultValue().equals(String.valueOf(configuredFetchSize)) ? 1 : configuredFetchSize);
    }

    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
