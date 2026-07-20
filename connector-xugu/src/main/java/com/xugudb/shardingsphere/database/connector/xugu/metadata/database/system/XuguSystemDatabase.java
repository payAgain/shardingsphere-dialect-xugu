package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.system;

import org.apache.shardingsphere.database.connector.core.metadata.database.system.DialectSystemDatabase;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System database of XuGu.
 */
public final class XuguSystemDatabase implements DialectSystemDatabase {
    
    private static final Map<String, Collection<String>> SYSTEM_DATABASE_SCHEMA_MAP = new LinkedHashMap<>();
    
    static {
        SYSTEM_DATABASE_SCHEMA_MAP.put("SYSTEM", Collections.singleton("SYSTEM"));
        SYSTEM_DATABASE_SCHEMA_MAP.put("sysdba", Collections.singleton("sysdba"));
        SYSTEM_DATABASE_SCHEMA_MAP.put("shardingsphere", Collections.singleton("shardingsphere"));
    }
    
    @Override
    public Collection<String> getSystemDatabases() {
        return SYSTEM_DATABASE_SCHEMA_MAP.keySet();
    }
    
    @Override
    public Collection<String> getSystemSchemas(final String databaseName) {
        return SYSTEM_DATABASE_SCHEMA_MAP.getOrDefault(databaseName, Collections.emptyList());
    }
    
    @Override
    public Collection<String> getSystemSchemas() {
        return SYSTEM_DATABASE_SCHEMA_MAP.keySet();
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
