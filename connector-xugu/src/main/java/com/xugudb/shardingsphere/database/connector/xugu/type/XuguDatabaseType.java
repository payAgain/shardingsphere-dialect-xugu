package com.xugudb.shardingsphere.database.connector.xugu.type;

import org.apache.shardingsphere.database.connector.core.type.DatabaseType;

import java.util.Collection;
import java.util.Collections;

public final class XuguDatabaseType implements DatabaseType {

    @Override
    public Collection<String> getJdbcUrlPrefixes() {
        return Collections.singleton("jdbc:xugu:");
    }

    @Override
    public String getType() {
        return "XuGu";
    }
}
