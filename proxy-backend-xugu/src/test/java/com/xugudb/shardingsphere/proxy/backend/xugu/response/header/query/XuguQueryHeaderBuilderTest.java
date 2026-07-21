package com.xugudb.shardingsphere.proxy.backend.xugu.response.header.query;

import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeaderBuilder;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XuguQueryHeaderBuilderTest {

    private final DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "XuGu");

    private final QueryHeaderBuilder queryHeaderBuilder = DatabaseTypedSPILoader.getService(QueryHeaderBuilder.class, databaseType);

    @Test
    void assertGetDatabaseType() {
        assertThat(new XuguQueryHeaderBuilder().getDatabaseType(), is("XuGu"));
    }

    @Test
    void assertBuild() throws SQLException {
        QueryResultMetaData queryResultMetaData = mock(QueryResultMetaData.class);
        when(queryResultMetaData.getColumnType(1)).thenReturn(Types.INTEGER);
        when(queryResultMetaData.getColumnTypeName(1)).thenReturn("int");
        when(queryResultMetaData.getColumnLength(1)).thenReturn(11);
        QueryHeader actual = queryHeaderBuilder.build(queryResultMetaData, null, null, "foo_label", 1);
        assertThat(actual.getSchema(), is(""));
        assertThat(actual.getTable(), is(""));
        assertThat(actual.getColumnLabel(), is("foo_label"));
        assertThat(actual.getColumnName(), is(""));
        assertThat(actual.getColumnType(), is(Types.INTEGER));
        assertThat(actual.getColumnTypeName(), is("int"));
        assertThat(actual.getColumnLength(), is(11));
        assertThat(actual.getDecimals(), is(0));
    }
}
