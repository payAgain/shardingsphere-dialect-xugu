package com.xugudb.shardingsphere.infra.binder.xugu.bind;

import org.apache.shardingsphere.infra.binder.engine.DialectSQLBindEngine;
import org.apache.shardingsphere.infra.binder.engine.statement.SQLStatementBinderContext;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

import java.util.Optional;

/**
 * SQL bind engine for XuGu (stub: no dialect-specific DAL binding; Oracle parity).
 */
public final class XuguSQLBindEngine implements DialectSQLBindEngine {
    
    @Override
    public Optional<SQLStatement> bind(final SQLStatement sqlStatement, final SQLStatementBinderContext binderContext) {
        return Optional.empty();
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
