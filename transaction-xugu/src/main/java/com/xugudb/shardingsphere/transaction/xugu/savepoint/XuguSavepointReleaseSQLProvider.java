package com.xugudb.shardingsphere.transaction.xugu.savepoint;

import org.apache.shardingsphere.transaction.savepoint.SavepointReleaseSQLProvider;

/**
 * Savepoint release SQL provider for XuGu.
 */
public final class XuguSavepointReleaseSQLProvider implements SavepointReleaseSQLProvider {
    
    @Override
    public String getSQL(final String savepointName) {
        return String.format("RELEASE SAVEPOINT %s", savepointName);
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
