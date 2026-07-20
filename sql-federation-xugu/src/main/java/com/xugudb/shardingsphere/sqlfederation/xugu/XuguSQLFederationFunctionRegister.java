package com.xugudb.shardingsphere.sqlfederation.xugu;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.shardingsphere.sqlfederation.compiler.sql.function.DialectSQLFederationFunctionRegister;

/**
 * SQL federation function register for XuGu.
 *
 * <p>NONE compatible mode uses Oracle-like Calcite library functions via connection config.
 * Keep dialect-specific registration empty until XuGu-verified custom functions are needed.
 */
public final class XuguSQLFederationFunctionRegister implements DialectSQLFederationFunctionRegister {
    
    @Override
    public void registerFunction(final SchemaPlus schemaPlus, final String schemaName) {
        // Safe minimal set: no MySQL-specific BIN; Oracle FUN already provided by connection config.
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
