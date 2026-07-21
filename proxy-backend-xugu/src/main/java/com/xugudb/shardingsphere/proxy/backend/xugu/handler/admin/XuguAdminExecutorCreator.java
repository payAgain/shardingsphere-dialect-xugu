package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin;

import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguSetVariableAdminExecutor;
import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguShowVariableExecutor;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutorCreator;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.SetStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.ShowStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

import java.util.List;
import java.util.Optional;

/**
 * Database admin executor creator for XuGu.
 */
public final class XuguAdminExecutorCreator implements DatabaseAdminExecutorCreator {

    @Override
    public Optional<DatabaseAdminExecutor> create(final SQLStatementContext sqlStatementContext, final String sql,
                                                  final String databaseName, final List<Object> parameters) {
        SQLStatement sqlStatement = sqlStatementContext.getSqlStatement();
        if (sqlStatement instanceof SelectStatement) {
            return Optional.empty();
        }
        if (sqlStatement instanceof SetStatement) {
            return Optional.of(new XuguSetVariableAdminExecutor((SetStatement) sqlStatement));
        }
        if (sqlStatement instanceof ShowStatement) {
            return Optional.of(new XuguShowVariableExecutor((ShowStatement) sqlStatement));
        }
        return Optional.empty();
    }

    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
