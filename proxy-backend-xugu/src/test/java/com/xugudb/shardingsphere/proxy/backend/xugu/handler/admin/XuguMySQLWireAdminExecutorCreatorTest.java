package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin;

import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguMySQLSystemVariableQueryExecutor;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.CommonSQLStatementContext;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutorCreator;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XuguMySQLWireAdminExecutorCreatorTest {

    private static final DatabaseType MYSQL = TypedSPILoader.getService(DatabaseType.class, "MySQL");

    private final DatabaseAdminExecutorCreator creator =
            DatabaseTypedSPILoader.getService(DatabaseAdminExecutorCreator.class, MYSQL);

    @Test
    void assertGetDatabaseType() {
        assertThat(new XuguMySQLWireAdminExecutorCreator().getDatabaseType(), is("MySQL"));
    }

    @Test
    void assertMySQLSessionVariableSelect() {
        SelectStatement sqlStatement = new SelectStatement(MYSQL);
        sqlStatement.buildAttributes();
        SQLStatementContext ctx = new CommonSQLStatementContext(sqlStatement);
        Optional<DatabaseAdminExecutor> actual = creator.create(ctx,
                "SELECT @@session.auto_increment_increment AS auto_increment_increment, "
                        + "@@character_set_client AS character_set_client",
                "", Collections.emptyList());
        assertTrue(actual.isPresent());
        assertThat(actual.get(), isA(XuguMySQLSystemVariableQueryExecutor.class));
    }

    @Test
    void assertPlainSelectReturnsEmpty() {
        SelectStatement sqlStatement = new SelectStatement(MYSQL);
        sqlStatement.buildAttributes();
        SQLStatementContext ctx = new CommonSQLStatementContext(sqlStatement);
        Optional<DatabaseAdminExecutor> actual = creator.create(ctx, "SELECT 1", "", Collections.emptyList());
        assertFalse(actual.isPresent());
    }
}
