package com.xugudb.shardingsphere.infra.route.xugu;

import org.apache.shardingsphere.infra.route.engine.tableless.DialectDALStatementBroadcastRouteDecider;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.DALStatement;

/**
 * Dialect DAL statement broadcast route decider for XuGu.
 * Minimal stub: XuGu has no MySQL-style resource-group / allow-not-use-database DAL that needs broadcast.
 */
public final class XuguDALStatementBroadcastRouteDecider implements DialectDALStatementBroadcastRouteDecider {
    
    @Override
    public boolean isDataSourceBroadcastRoute(final DALStatement sqlStatement) {
        return false;
    }
    
    @Override
    public boolean isInstanceBroadcastRoute(final DALStatement sqlStatement) {
        return false;
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
