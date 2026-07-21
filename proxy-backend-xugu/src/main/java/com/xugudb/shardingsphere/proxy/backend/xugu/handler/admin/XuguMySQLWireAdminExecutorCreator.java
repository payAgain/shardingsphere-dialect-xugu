package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin;

import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguMySQLSystemVariableQueryExecutor;
import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguMySQLWireSetVariableAdminExecutor;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutorCreator;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.SetStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;

import java.util.List;
import java.util.Optional;

/**
 * MySQL-wire admin executor creator for Proxy when storage is XuGu.
 *
 * <p>Proxy looks up {@link DatabaseAdminExecutorCreator} by the <em>frontend protocol</em>
 * database type ({@code MySQL}), not the storage type. Without a MySQL-typed creator,
 * Connector/J handshake {@code SELECT @@...} / {@code SET ...} is forwarded to XuGu and fails.</p>
 *
 * <p>This is intentionally <strong>not</strong> {@code proxy-backend-mysql}: storage dialect
 * remains XuGu {@code compatiblemode=NONE}.</p>
 */
public final class XuguMySQLWireAdminExecutorCreator implements DatabaseAdminExecutorCreator {

    @Override
    public Optional<DatabaseAdminExecutor> create(final SQLStatementContext sqlStatementContext, final String sql,
                                                  final String databaseName, final List<Object> parameters) {
        SQLStatement sqlStatement = sqlStatementContext.getSqlStatement();
        if (sqlStatement instanceof SelectStatement) {
            return XuguMySQLSystemVariableQueryExecutor.tryCreate((SelectStatement) sqlStatement, sql);
        }
        if (sqlStatement instanceof SetStatement) {
            return Optional.of(new XuguMySQLWireSetVariableAdminExecutor((SetStatement) sqlStatement));
        }
        return Optional.empty();
    }

    @Override
    public String getDatabaseType() {
        return "MySQL";
    }
}
