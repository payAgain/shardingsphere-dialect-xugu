package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.option;

import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.schema.DefaultSchemaOption;
import org.apache.shardingsphere.database.connector.core.metadata.database.metadata.option.schema.DialectSchemaOption;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Schema option for XuGu.
 */
public final class XuguSchemaOption implements DialectSchemaOption {
    
    // SS SchemaRefreshUtils lowercases schema keys; keep default lowercase to match metadata map lookups.
    private final DialectSchemaOption delegate = new DefaultSchemaOption(true, "sysdba");
    
    @Override
    public boolean isSchemaAvailable() {
        return delegate.isSchemaAvailable();
    }
    
    @Override
    public String getSchema(final Connection connection) {
        try {
            return Optional.ofNullable(connection.getSchema()).map(String::toUpperCase).orElse(null);
        } catch (final SQLException ignored) {
            return null;
        }
    }
    
    @Override
    public Optional<String> getDefaultSchema() {
        return delegate.getDefaultSchema();
    }
    
    @Override
    public Optional<String> getDefaultSystemSchema() {
        return delegate.getDefaultSystemSchema();
    }
}
