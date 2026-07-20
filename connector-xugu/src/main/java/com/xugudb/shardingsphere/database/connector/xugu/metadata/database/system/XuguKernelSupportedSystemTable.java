package com.xugudb.shardingsphere.database.connector.xugu.metadata.database.system;

import org.apache.shardingsphere.database.connector.core.metadata.database.system.DialectKernelSupportedSystemTable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Kernel supported system table for XuGu.
 */
public final class XuguKernelSupportedSystemTable implements DialectKernelSupportedSystemTable {
    
    @Override
    public Map<String, Collection<String>> getSchemaAndTablesMap() {
        return Collections.singletonMap("sysdba", new HashSet<>(Arrays.asList(
                "ALL_TABLES", "ALL_COLUMNS", "ALL_INDEXES", "ALL_CONSTRAINTS", "ALL_VIEWS", "ALL_SCHEMAS", "ALL_VIEW_COLUMNS")));
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
