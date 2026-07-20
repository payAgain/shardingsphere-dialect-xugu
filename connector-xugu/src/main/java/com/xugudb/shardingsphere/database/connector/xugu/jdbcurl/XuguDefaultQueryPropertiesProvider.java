package com.xugudb.shardingsphere.database.connector.xugu.jdbcurl;

import org.apache.shardingsphere.database.connector.core.jdbcurl.DialectDefaultQueryPropertiesProvider;

import java.util.Properties;

/**
 * Default query properties provider for XuGu.
 */
public final class XuguDefaultQueryPropertiesProvider implements DialectDefaultQueryPropertiesProvider {
    
    @Override
    public Properties getDefaultQueryProperties() {
        Properties result = new Properties();
        result.setProperty("compatiblemode", "NONE");
        result.setProperty("charset", "UTF8");
        return result;
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
