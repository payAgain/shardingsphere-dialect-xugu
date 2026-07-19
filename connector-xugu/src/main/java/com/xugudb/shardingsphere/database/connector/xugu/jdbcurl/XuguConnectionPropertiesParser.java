package com.xugudb.shardingsphere.database.connector.xugu.jdbcurl;

import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.ConnectionProperties;
import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.ConnectionPropertiesParser;
import org.apache.shardingsphere.database.connector.core.jdbcurl.parser.StandardJdbcUrlParser;

public final class XuguConnectionPropertiesParser implements ConnectionPropertiesParser {

    private static final int DEFAULT_PORT = 5138;

    @Override
    public ConnectionProperties parse(final String url, final String username, final String catalog) {
        ConnectionProperties properties = new StandardJdbcUrlParser().parse(url, DEFAULT_PORT);
        String currentSchema = properties.getQueryProperties().getProperty("current_schema");
        return new ConnectionProperties(properties.getHostname(), properties.getPort(),
                null == catalog ? properties.getCatalog() : catalog,
                null == currentSchema ? username : currentSchema,
                properties.getQueryProperties());
    }

    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
