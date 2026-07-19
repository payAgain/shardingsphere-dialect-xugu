package com.xugudb.shardingsphere.database.connector.xugu.resultset;

import org.apache.shardingsphere.database.connector.core.resultset.DialectResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Result set mapper of XuGu.
 */
public final class XuguResultSetMapper implements DialectResultSetMapper {
    
    @Override
    public Object getSmallintValue(final ResultSet resultSet, final int columnIndex) throws SQLException {
        return resultSet.getInt(columnIndex);
    }
    
    @Override
    public Object getDateValue(final ResultSet resultSet, final int columnIndex) throws SQLException {
        return resultSet.getDate(columnIndex);
    }
    
    @Override
    public String getDatabaseType() {
        return "XuGu";
    }
}
