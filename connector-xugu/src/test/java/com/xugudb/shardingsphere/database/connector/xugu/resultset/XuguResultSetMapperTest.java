package com.xugudb.shardingsphere.database.connector.xugu.resultset;

import org.apache.shardingsphere.database.connector.core.resultset.DialectResultSetMapper;
import org.apache.shardingsphere.database.connector.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.database.connector.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XuguResultSetMapperTest {
    
    private final DialectResultSetMapper dialectResultSetMapper = DatabaseTypedSPILoader.getService(
            DialectResultSetMapper.class, TypedSPILoader.getService(DatabaseType.class, "XuGu"));
    
    @Test
    void assertGetSmallintValue() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt(1)).thenReturn(7);
        assertThat(dialectResultSetMapper.getSmallintValue(resultSet, 1), is(7));
        verify(resultSet).getInt(1);
    }
    
    @Test
    void assertGetDateValue() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        Date expected = new Date(0L);
        when(resultSet.getDate(1)).thenReturn(expected);
        assertThat(dialectResultSetMapper.getDateValue(resultSet, 1), is(expected));
        verify(resultSet).getDate(1);
    }
    
    @Test
    void assertGetDatabaseType() {
        assertThat(dialectResultSetMapper.getDatabaseType(), is("XuGu"));
    }
    
    @Test
    void assertGetSmallintValueUsesGetIntNotGetShort() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt(2)).thenReturn(32767);
        assertThat(dialectResultSetMapper.getSmallintValue(resultSet, 2), is(32767));
        verify(resultSet).getInt(2);
    }
    
    @Test
    void assertGetDateValuePropagatesNull() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getDate(1)).thenReturn(null);
        assertThat(dialectResultSetMapper.getDateValue(resultSet, 1), is((Date) null));
    }
}
