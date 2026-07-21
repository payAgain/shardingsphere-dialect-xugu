package com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin;

import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguSetVariableAdminExecutor;
import com.xugudb.shardingsphere.proxy.backend.xugu.handler.admin.executor.XuguShowVariableExecutor;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.CommonSQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.statement.type.dml.DeleteStatementContext;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutor;
import org.apache.shardingsphere.proxy.backend.handler.admin.executor.DatabaseAdminExecutorCreator;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.SetStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dal.ShowStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.DeleteStatement;
import org.apache.shardingsphere.sql.parser.statement.core.statement.type.dml.SelectStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XuguAdminExecutorCreatorTest {

    private static final DatabaseType DATABASE_TYPE = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    private final DatabaseAdminExecutorCreator creator = DatabaseTypedSPILoader.getService(DatabaseAdminExecutorCreator.class, DATABASE_TYPE);

    @Test
    void assertGetDatabaseType() {
        assertThat(new XuguAdminExecutorCreator().getDatabaseType(), is("XuGu"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("createArguments")
    void assertCreate(final String name, final SQLStatementContext sqlStatementContext, final String sql,
                      final Class<? extends DatabaseAdminExecutor> expectedExecutorType) {
        Optional<DatabaseAdminExecutor> actual = creator.create(sqlStatementContext, sql, "", Collections.emptyList());
        if (null == expectedExecutorType) {
            assertFalse(actual.isPresent(), name);
        } else {
            assertTrue(actual.isPresent(), name);
            assertThat(actual.get(), isA(expectedExecutorType));
        }
    }

    private static Stream<Arguments> createArguments() {
        return Stream.of(
                Arguments.of("select statement returns empty", createSelectStatementContext(), "SELECT 1", null),
                Arguments.of("set statement returns set executor", createSetStatementContext(), "SET client_encoding = utf8", XuguSetVariableAdminExecutor.class),
                Arguments.of("show statement returns show executor", createShowStatementContext(), "SHOW server_version", XuguShowVariableExecutor.class),
                Arguments.of("delete statement returns empty", createOtherStatementContext(), "DELETE FROM t WHERE id = 1", null));
    }

    private static SQLStatementContext createSetStatementContext() {
        SetStatement sqlStatement = new SetStatement(DATABASE_TYPE, Collections.emptyList());
        sqlStatement.buildAttributes();
        return new CommonSQLStatementContext(sqlStatement);
    }

    private static SQLStatementContext createShowStatementContext() {
        ShowStatement sqlStatement = new ShowStatement(DATABASE_TYPE, "server_version");
        sqlStatement.buildAttributes();
        return new CommonSQLStatementContext(sqlStatement);
    }

    private static SQLStatementContext createSelectStatementContext() {
        SelectStatement sqlStatement = new SelectStatement(DATABASE_TYPE);
        sqlStatement.buildAttributes();
        return new CommonSQLStatementContext(sqlStatement);
    }

    private static SQLStatementContext createOtherStatementContext() {
        return new DeleteStatementContext(new DeleteStatement(DATABASE_TYPE));
    }
}
