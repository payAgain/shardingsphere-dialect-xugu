package com.xugudb.shardingsphere.proxy.backend.xugu.connector.jdbc.statement;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.config.props.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.proxy.backend.connector.jdbc.statement.StatementMemoryStrictlyFetchSizeSetter;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XuguStatementMemoryStrictlyFetchSizeSetterTest {

    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    private final StatementMemoryStrictlyFetchSizeSetter fetchSizeSetter = DatabaseTypedSPILoader.getService(StatementMemoryStrictlyFetchSizeSetter.class, databaseType);

    @AfterEach
    void tearDown() {
        ProxyContext.init(null);
    }

    @Test
    void assertGetDatabaseType() {
        assertThat(new XuguStatementMemoryStrictlyFetchSizeSetter().getDatabaseType(), is("XuGu"));
    }

    @Test
    void assertSpiLoaded() {
        assertThat(fetchSizeSetter, is(notNullValue()));
        assertThat(fetchSizeSetter.getDatabaseType(), is("XuGu"));
    }

    @Test
    void assertSetFetchSizeWithDefaultValue() throws SQLException {
        Statement statement = mock(Statement.class);
        ProxyContext.init(mockContextManager(-1));
        fetchSizeSetter.setFetchSize(statement);
        verify(statement).setFetchSize(1);
    }

    @Test
    void assertSetFetchSizeWithCustomValue() throws SQLException {
        Statement statement = mock(Statement.class);
        ProxyContext.init(mockContextManager(20));
        fetchSizeSetter.setFetchSize(statement);
        verify(statement).setFetchSize(20);
    }

    private ContextManager mockContextManager(final int configuredFetchSize) {
        ContextManager result = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        when(result.getMetaDataContexts().getMetaData().getProps().<Integer>getValue(ConfigurationPropertyKey.PROXY_BACKEND_QUERY_FETCH_SIZE))
                .thenReturn(configuredFetchSize);
        return result;
    }
}
